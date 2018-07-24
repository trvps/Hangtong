package com.htdz.gateway;


import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.htdz.common.LogManager;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Service
public class SafeEngine  {
	public interface AttackListener {
		// 检测到特定连接攻击
		public void detectedAttack(String ip, int port, String tag);
		// 检测到特定IP攻击
		public void detectedAttack(String ip);
	}
	
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	private static class ADInfo {
		private long timestamp;
		private String ip;
		private int port;
		private String tag;
		private byte[] data;
	}
	
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	private static class ADIPItem {
		private long timestamp_start;
		private long timestamp_resent;
		private String ip;
	
		private int attackCount = 0;
		
		private Map<Integer, ADPortItem> adItemMap = new HashMap<Integer, ADPortItem>();
	}
	
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	private static class ADPortItem {
		private long timestamp_start;
		private long timestamp_resent;
		private int port;
		private String tag;
		
		private int attackCount = 0;
	}
	
	
	// 定期清理时间间隔 default: 10*60*1000
	@Value("${safe.timeout.cleanup:600000}")
	private Integer timeout_cleanup;
	
	// 请求记录失效时长 default: 30*60*1000
	@Value("${safe.timeout.adrecord:1800000}")
	private int timeout_adrecord;
	
	// IP黑名单失效时长 default: 30*60*1000
	@Value("${safe.timeout.ipblack:1800000}")
	private int timeout_ipblack;
	
	// 相同IP的检测单位时长 default: 60*1000
	@Value("${safe.ip.attackdetect.timespace:60000}")
	private int ip_attack_detect_timespace;
	
	// 相同IP的单位时长最大请求次数 default: 1000
	@Value("${safe.ip.attackdetect.maxcount:1000}")
	private int ip_attack_detect_maxcount;
	
	// 相同IP,端口的检测单位时长 default: 2*1000
	@Value("${safe.ipport.attackdetect.timespace:2000}")
	private int ip_port_attack_detect_timespace;
	
	// 相同IP,端口的单位时长最大请求次数 default: 10
	@Value("${safe.ipport.attackdetect.maxcount:10}")
	private int ip_port_attack_detect_maxcount;
	

	// 请求记录
	private Map<String, ADIPItem> adIPItemMap = new HashMap<String, ADIPItem>();
	
	// 上次清理时间戳
	private long lastCleanUpTimestamp = System.currentTimeMillis();
	
	// IP黑名单记录
	private Map<String, Long> ipBlackMap = new HashMap<String, Long>();
	private ReentrantReadWriteLock ipBlackLock = new ReentrantReadWriteLock();
	private ReadLock ipBlackReadLock = ipBlackLock.readLock();
	private WriteLock ipBlackWriteLock = ipBlackLock.writeLock();
	
	// 待处理的请求记录
	private ArrayBlockingQueue<ADInfo> dadInfoQueue = new ArrayBlockingQueue<ADInfo>(1000);
	private ExecutorService exector = Executors.newFixedThreadPool(1);
	
	private AttackListener attackListener = null;
	
	/**
	 * 设置定时清理时长
	 * @param timeout_cleanup
	 */
	public void setTimeOut_CleanUP(int timeout_cleanup) {
		this.timeout_cleanup = timeout_cleanup;
	}
	
	/**
	 * 设置记录保存时长
	 * @param timeout_adrecord
	 */
	public void setTimeOut_ADRecord(int timeout_adrecord) {
		this.timeout_adrecord = timeout_adrecord;
	}
	
	/**
	 * 设置IP黑名单失效时长
	 * @param timeout_ipblack
	 */
	public void setTimeOut_IPBlack(int timeout_ipblack) {
		this.timeout_ipblack = timeout_ipblack;
	}
	
	/**
	 * 设置相同IP的检测单位时长
	 * @param ip_attack_detect_timespace
	 */
	public void setIP_Attack_Detect_TimeSpace(int ip_attack_detect_timespace) {
		this.ip_attack_detect_timespace = ip_attack_detect_timespace;
	}
	
	/**
	 * 设置相同IP的单位时长最大请求次数
	 * @param ip_attack_detect_maxcount
	 */
	public void setIP_Attack_Detect_MaxCount(int ip_attack_detect_maxcount) {
		this.ip_attack_detect_maxcount = ip_attack_detect_maxcount;
	}
	
	/**
	 * 设置相同IP,端口的检测单位时长
	 * @param ip_port_attack_detect_timespace
	 */
	public void setIP_PORT_Attack_Detect_TimeSpace(int ip_port_attack_detect_timespace) {
		this.ip_port_attack_detect_timespace = ip_port_attack_detect_timespace;
	}
	
	/**
	 * 设置相同IP,端口的单位时长最大请求次数
	 * @param ip_port_attack_detect_maxcount
	 */
	public void setIP_PORT_Attack_Detect_MaxCount(int ip_port_attack_detect_maxcount) {
		this.ip_port_attack_detect_maxcount = ip_port_attack_detect_maxcount;
	}
	
	/**
	 * 设置攻击监听器
	 * @param attackListener
	 */
	public void setAttackListener(AttackListener attackListener) {
		this.attackListener = attackListener;
	}
	
	public void onContextInitCompleted() {
		exector.execute(new DeviceAttackDetectTask());
	}
	
	/**
	 * 添加到防攻击检测系统
	 * @param ip
	 * @param port
	 * @param data
	 * @param tag
	 */
	public void addToAttackDetect(String ip, 
									int port, 
									byte[] data, 
									String tag) {
		try {
			ADInfo adInfo = new ADInfo();
			adInfo.setIp(ip);
			adInfo.setPort(port);
			adInfo.setTag(tag);
			adInfo.setData(data);
			adInfo.setTimestamp(System.currentTimeMillis());
			
			dadInfoQueue.put(adInfo);
		} catch (Exception e) {
			LogManager.exception(e.getMessage(), e);
		}
	}
	
	/**
	 * 检测指定ip是否发起过攻击
	 * @param ip
	 * @return
	 */
	public boolean deviceAttacked(String ip) {
		try {
			ipBlackReadLock.lock();
			Long l = ipBlackMap.get(ip);
			if (l != null) {
				// 判断黑名单记录是否超过有效期
				if (System.currentTimeMillis() - l <= timeout_ipblack) {
					return true;
				}
			}
		} finally {
			ipBlackReadLock.unlock();
		}
		
		return false;
	}
	
	/**
	 * 记录IP到黑名单
	 */
	private void recordIPBlack(String ip) {
		try {
			ipBlackWriteLock.lock();
			ipBlackMap.put(ip, System.currentTimeMillis());
		} finally {
			ipBlackWriteLock.unlock();
		}
	}
	
	/**
	 * 清理IP黑名单
	 */
	private void cleanUp_IPBlack() {
		try {
			ipBlackWriteLock.lock();

			Iterator<Entry<String, Long>> iterator = ipBlackMap.entrySet().iterator();
			while (iterator.hasNext()) {
				Entry<String, Long> entry = iterator.next();
				
				// 删除超时IP黑名单
				if (System.currentTimeMillis() - entry.getValue() > timeout_ipblack)
					iterator.remove();
			}
		} finally {
			ipBlackWriteLock.unlock();
		}
	}
	
	/**
	 * 清理过期检测数据
	 */
	private void cleanUp_ADIPItem() {
		try {
			Iterator<Entry<String, ADIPItem>> iterator = adIPItemMap.entrySet().iterator();
			while (iterator.hasNext()) {
				Entry<String, ADIPItem> entry = iterator.next();
				
				ADIPItem adIPItem = entry.getValue();
				
				// 删除超时IP设备记录
				if (System.currentTimeMillis() - adIPItem.timestamp_resent >= timeout_adrecord) {
					iterator.remove();
				} else {
					Iterator<Entry<Integer, ADPortItem>> _iterator = adIPItem.getAdItemMap().entrySet().iterator();
					while (_iterator.hasNext()) {
						Entry<Integer, ADPortItem> _entry = _iterator.next();
						
						ADPortItem adPortItem = _entry.getValue();
						
						// 删除超时PORT设备记录
						if (System.currentTimeMillis() - adPortItem.getTimestamp_resent() >= timeout_adrecord) {
							_iterator.remove();
						}
					}
				}
			}
		} catch (Exception e) {
			LogManager.exception(e.getMessage(), e);
		}
	}
	
	/**
	 * 攻击检测
	 */
	private void attackDetect(ADInfo adInfo) {
		String ip = adInfo.getIp();
		int port = adInfo.getPort();
		long timestamp = adInfo.getTimestamp();
		
		// 数据量超大，比如10M以上，确定为攻击
		if (adInfo.getData() != null && adInfo.getData().length > 10 * 1024 * 1024) {
			// 检测到特定连接大数据攻击，通知观测器有攻击
			LogManager.info("检测到设备大数据攻击 ip={}, port={}, tag={} len={}", 
							ip, 
							port, 
							adInfo.getTag(), 
							adInfo.getData().length);
			
			if (attackListener != null)
				attackListener.detectedAttack(ip, port, adInfo.getTag());
		}
		
		ADIPItem adIPItem = adIPItemMap.get(ip);
		if (adIPItem == null) {
			// 添加新记录
			ADPortItem adPortItem = new ADPortItem();
			adPortItem.setPort(adInfo.getPort());
			adPortItem.setTimestamp_start(timestamp);
			adPortItem.setTimestamp_resent(timestamp);
			adPortItem.setTag(adInfo.getTag());
			adPortItem.setAttackCount(0);
			
			adIPItem = new ADIPItem();
			adIPItem.setIp(ip);
			adIPItem.setTimestamp_start(timestamp);
			adIPItem.setTimestamp_resent(timestamp);
			adIPItem.getAdItemMap().put(adInfo.getPort(), adPortItem);
			
			adIPItemMap.put(ip, adIPItem);
		} else {
			// 短时间大量请求，确定为攻击
			
			// 检测是否为特定链接的恶意攻击
			ADPortItem adPortItem = adIPItem.getAdItemMap().get(port);
			if (adPortItem != null) {
				if (timestamp - adPortItem.getTimestamp_start() < ip_port_attack_detect_timespace) {
					// 在检测时间片段内
					
					int attackCount = adPortItem.getAttackCount() + 1;
					if (attackCount >= ip_port_attack_detect_maxcount) {
						// 检测到特定连接攻击，通知观测器有攻击
						LogManager.info("检测到设备攻击 ip={}, port={}, tag={}", 
											ip, 
											port, 
											adPortItem.getTag());
						
						if (attackListener != null)
							attackListener.detectedAttack(ip, port, adPortItem.getTag());
						
						// 重置
						//LogManager.info("---reset--- ip {} port {}", ip, port);
						adPortItem.setTimestamp_start(timestamp);
						adPortItem.setTimestamp_resent(timestamp);
						adPortItem.setAttackCount(0);
					} else {
						adPortItem.setTimestamp_resent(timestamp);
						adPortItem.setAttackCount(attackCount);
					}
				} else {
					// 在检测时间片段外，重置
					//LogManager.info("---reset--- ip {} port {}", ip, port);
					adPortItem.setTimestamp_start(timestamp);
					adPortItem.setTimestamp_resent(timestamp);
					adPortItem.setAttackCount(0);
				}
			} else {
				// 添加记录
				adPortItem = new ADPortItem();
				adPortItem.setPort(port);
				adPortItem.setTag(adInfo.getTag());
				adPortItem.setTimestamp_start(timestamp);
				adPortItem.setTimestamp_resent(timestamp);
				adPortItem.setAttackCount(0);
				adIPItem.getAdItemMap().put(port, adPortItem);
			}
			
			// 检测是否特定IP的恶意攻击
			if (timestamp - adIPItem.getTimestamp_start() < ip_attack_detect_timespace) {
				// 在检测时间片段内
				
				int attackCount = adIPItem.getAttackCount() + 1;
				if (attackCount >= ip_attack_detect_maxcount) {
					// 检测到特定IP攻击，通知观测器有攻击
					LogManager.info("检测到IP攻击 ip={}", ip);
					
					if (attackListener != null)
						attackListener.detectedAttack(ip);
					
					// 加入IP黑名单
					recordIPBlack(ip);
					
					// 重置
					//LogManager.info("---reset--- ip {}", ip);
					
					adIPItem.setTimestamp_start(timestamp);
					adIPItem.setTimestamp_resent(timestamp);
					adIPItem.setAttackCount(0);
				} else {
					adIPItem.setTimestamp_resent(timestamp);
					adIPItem.setAttackCount(attackCount);
				}
			} else {
				// 在检测时间片段外，重置
				//LogManager.info("---reset--- ip {}", ip);
				
				adIPItem.setTimestamp_start(timestamp);
				adIPItem.setTimestamp_resent(timestamp);
				adIPItem.setAttackCount(0);
			}
		}
	}
	
	/**
	 *  清理过期数据
	 */
	private void cleanUp() {
		if (System.currentTimeMillis() - lastCleanUpTimestamp < timeout_cleanup) {
			// 未到清理时间
			return ;
		}
		
		LogManager.info("安全引擎清理超时无效数据...");
		
		lastCleanUpTimestamp = System.currentTimeMillis();
		
		// 清理IP黑名单
		cleanUp_IPBlack();
		
		// 清理过期检测数据
		cleanUp_ADIPItem();
	}
	
	private class DeviceAttackDetectTask implements Runnable {
		@Override
		public void run() {
			while (true) {
				try {
					ADInfo adInfo = dadInfoQueue.take();
					attackDetect(adInfo);
					
					cleanUp();
					
					Thread.yield();
				} catch (Exception e) {
					LogManager.exception(e.getMessage(), e);
				}
			}
		}
	}
	
	public static void main(String args[]) {
		LogManager.info("---------- SafeEngine main ----------");
		
		final String ip = "127.0.0.1";
		final int port = 3000;
		final byte[] data = "test".getBytes();
		final String tag = "device";
		
		SafeEngine safeEngine = new SafeEngine();
		safeEngine.onContextInitCompleted();
		
		safeEngine.setAttackListener(new AttackListener() {
			@Override
			public void detectedAttack(String ip, int port, String tag) {
				LogManager.info("detectedAttack ip={}, port={}, tag={}", ip, port, tag);
			}

			@Override
			public void detectedAttack(String ip) {
				LogManager.info("detectedAttack ip={}", ip);
			}
		});
		
		new Thread() {
			public void run() {
				while (true) {
					try {
						LogManager.info("{} send--- ip={}, port={}, tag={}", 
										System.currentTimeMillis(), 
										ip, 
										port, 
										tag);
						safeEngine.addToAttackDetect(ip, port, data, tag);
						
						int rand = new Random().nextInt(100);
						TimeUnit.MILLISECONDS.sleep(150 + rand);
					} catch (Exception e) {
						LogManager.exception(e.getMessage(), e);
					}
				}
			}
		}.start();
	}
}

