package com.htdz.device.handler;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.htdz.common.Consts;
import com.htdz.common.LogManager;
import com.htdz.common.utils.MapUtils;
import com.htdz.common.utils.SpringContextUtil;
import com.htdz.db.service.PFriendService;
import com.htdz.def.data.Errors;
import com.htdz.def.data.RPCResult;
import com.htdz.def.interfaces.GatewaySerivce;
import com.htdz.device.data.Message790;

import lombok.Data;


@Data
public class PPFriendTask {
	// 数据库相关变量通过构造函数传入
	@Data
	private static class PPInfo {
		private String factoryName;
		private String deviceName;
		private String deviceSN;
		private String deviceSession;
		private long time;
		private double latitude;
		private double longitude;
		
		public boolean isTimeOut() {
			return (System.currentTimeMillis() - time) >= SPACE_DISTANCE;
		}
	}
	
	public static final int SPACE_TIME = 10*1000;
	public static final int SPACE_DISTANCE = 500;
	
	private BlockingQueue<PPInfo> queue = new LinkedBlockingQueue<PPInfo>(100);
	private ExecutorService executor = Executors.newFixedThreadPool(1);
	
	private List<PPInfo> ppList = new LinkedList<PPInfo>();
	private Lock lock = new ReentrantLock();
	
	public PPFriendTask() {
		executor.execute(new Runnable() {
			public void run() {
				while (true) {
					try {
						PPInfo ppInfo = queue.take();
						dealPPInfo(ppInfo);
					} catch (Exception e) {
						LogManager.exception(e.getMessage(), e);
					}
				}
			}
		});
	}
	
	private PFriendService getDeviceFriendService() {
		return (PFriendService)SpringContextUtil.getBean(PFriendService.class);
	}
	
	private GatewaySerivce getGatewayService() {
		return (GatewaySerivce)SpringContextUtil.getBean(GatewaySerivce.class);
	}
	
	public void addToPPList(String deviceName, 
								String factoryName, 
								String deviceSN,
								String deviceSession,
								long time, 
								double latitude, 
								double longitude) {
		try {
			PPInfo ppInfo = new PPInfo();
			ppInfo.setFactoryName(factoryName);
			ppInfo.setDeviceName(deviceName);
			ppInfo.setDeviceSN(deviceSN);
			ppInfo.setDeviceSession(deviceSession);
			ppInfo.setTime(time);
			ppInfo.setLatitude(latitude);
			ppInfo.setLongitude(longitude);
			queue.put(ppInfo);
		} catch (Exception e) {
			LogManager.exception(e.getMessage(), e);
		}
	}
	
	public void dealPPInfo(PPInfo ppInfo) {
		lock.lock();
		try {
			// 判断是否已在列表，如果在则抛弃上次数据
			PPInfo _ppInfo = findByDeviceSN(ppInfo.getDeviceSN());
			if (_ppInfo != null) {
				ppList.remove(_ppInfo);
			}
			
			// 查找附近朋友
			List<PPInfo> friendPPInfoList = findFriend(ppInfo);
			if (friendPPInfoList.size() == 0) {
				// 没有找到朋友，加入数组
				ppList.add(ppInfo);
			} else {
				// 找到近距离用户列表，处理相关交友逻辑
				PPInfo friendPPInfo = null;
				for (PPInfo _friendPPInfo : friendPPInfoList) {
					// 自己是否交过好友?
					// 对方是否交过好友?
					
					// 双方是否已是好友?
					boolean isFriend = getDeviceFriendService().isFriend(ppInfo.getDeviceSN(), _friendPPInfo.getDeviceSN());
					if (!isFriend) {
						friendPPInfo = _friendPPInfo;
						break;
					}
				}
				
				if (friendPPInfo != null) {
					boolean notifyAddFriend = notifyFriendAdd(ppInfo, friendPPInfo);
					if (notifyAddFriend) {
						// 添加好友关系至数据库
						boolean addFriend = getDeviceFriendService().Add(ppInfo.getDeviceSN(), friendPPInfo.getDeviceSN());
						if (addFriend) {
							LogManager.info("Add Friend: {} - {}", ppInfo.getDeviceSN(), friendPPInfo.getDeviceSN());
							// 如果交友成功，则从列表删除
							ppList.remove(friendPPInfo);
						} else {
							LogManager.info("Add Friend Failed: {} - {}", ppInfo.getDeviceSN(), friendPPInfo.getDeviceSN());
						}
					} else {
						LogManager.info("Notify Add Friend Failed: {} - {}", ppInfo.getDeviceSN(), friendPPInfo.getDeviceSN());
					}
				} else {
					// 否则，加入数组
					ppList.add(ppInfo);
				}
			}
		} finally {
			lock.unlock();
		}
	}
	
	public boolean notifyFriendAdd(PPInfo ppInfo1, PPInfo ppInfo2) {
		// 判断ppInfo.getDeviceSN()的设备类型，推送加好友信息
		boolean resulta = false;
		boolean resultb = false;
		
		if (ppInfo1.getDeviceName().equalsIgnoreCase(Consts.Device_HT790)) {
			resulta = notifyFriendAdd790(ppInfo1, ppInfo2.getDeviceSN());
			if (!resulta)
				return false;
		}
		
		if (ppInfo2.getDeviceName().equalsIgnoreCase(Consts.Device_HT790)) {
			resultb = notifyFriendAdd790(ppInfo2, ppInfo1.getDeviceSN());
			if (!resultb)
				return false;
		}
			
		if (resulta && resultb)
			return true;
		else
			return false;
	}
	
	private boolean notifyFriendAdd790(PPInfo ppInfo, String friendDeviceSN) {
		try {
			Message790 msg = new Message790();
			msg.setFactoryName(ppInfo.getFactoryName());
			msg.setDeviceSn(ppInfo.getDeviceSN());
			
			/**
			 * TODO:设备验证交友指令ppmf
			 */
			byte[] data = msg.buildBytesResponse("ppmf", friendDeviceSN.getBytes("UTF-8"));
			int count = pushNotifyFriendAdd(ppInfo.getDeviceName(), ppInfo.getDeviceSession(), data);
			if (count == 1) {
				return true;
			} else {
				// 通知设备ppInfo1添加好友失败
				LogManager.info("Notify {} Add Friend {} Failed!", ppInfo.getDeviceSN(), friendDeviceSN);
				return false;
			}
		} catch (Exception e) {
			LogManager.exception(e.getMessage(), e);
		}
		
		return false;
	}
	
	private int pushNotifyFriendAdd(String deviceName, String deviceSession, byte[] data) {
		RPCResult rpcResult = getGatewayService().pushMessageToDevice(deviceName, deviceSession, data);
		if (rpcResult.getRpcErrCode() == Errors.ERR_SUCCESS) {
			if (rpcResult.getRpcResult() != null) {
				int count = (Integer)rpcResult.getRpcResult();
				return count;
			}
		}
		
		return 0;
	}
	
	/**
	 * 查找指定项
	 * @param deviceSN
	 * @return
	 */
	public PPInfo findByDeviceSN(String deviceSN) {
		for (PPInfo ppInfo : ppList) {
			if (ppInfo.getDeviceSN().equalsIgnoreCase(deviceSN))
				return ppInfo;
		}
		
		return null;
	}
	
	/**
	 * 清除超时项
	 */
	public void removePPInfoTimeOut() {
		lock.lock();
		try {
			Iterator<PPInfo> iterator = ppList.iterator();
			while (iterator.hasNext()) {
				PPInfo ppInfo = iterator.next();
				if (ppInfo.isTimeOut()) {
					iterator.remove();
				} else {
					// 因为是按时间顺序排序的，所以如果当前项没有超时，则后续项不可能超时
					break;
				}
			}
		} finally {
			lock.unlock();
		}
	}
	
	/**
	 * 查找附近朋友
	 * @param ppInfo
	 * @return
	 */
	public List<PPInfo> findFriend(PPInfo ppInfo) {
		List<PPInfo> list = new ArrayList<PPInfo>(1);
		
		lock.lock();
		try {
			Iterator<PPInfo> iterator = ppList.iterator();
			while (iterator.hasNext()) {
				PPInfo _ppInfo = iterator.next();
				if (_ppInfo.isTimeOut()) {
					// 删除超时记录
					iterator.remove();
					continue;
				} 
				
				double distance = MapUtils.distance(_ppInfo.getLatitude(), 
														_ppInfo.getLongitude(), 
														ppInfo.getLatitude(), 
														ppInfo.getLongitude());
				if (distance < SPACE_DISTANCE) {
					list.add(ppInfo);
				}
			}
		} finally {
			lock.unlock();
		}
		
		// 按距离远近排序， 距离短的排前面
		Collections.sort(list, new Comparator<PPInfo>() {
			@Override
			public int compare(PPInfo ppInfo1, PPInfo ppInfo2) {
				double distance1 = MapUtils.distance(ppInfo1.getLatitude(), 
						ppInfo1.getLongitude(), 
						ppInfo.getLatitude(), 
						ppInfo.getLongitude());
				double distance2 = MapUtils.distance(ppInfo2.getLatitude(), 
						ppInfo2.getLongitude(), 
						ppInfo.getLatitude(), 
						ppInfo.getLongitude());
				
				return distance1 <= distance2 ? -1 : 1;
			}
		});
		
		return list;
	}
}
