package com.htdz.def.data;


import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


public class RouteInfo implements Serializable {
	private static final long serialVersionUID = 1L;
	
	// 路由信息记录链表
	private List<String> routelist = new LinkedList<String>();
	// 允许路由调数，默认为1
	private int ttl = 1;
	
	private RouteInfo(String routeInfo, int ttl) {
		addRouteInfo(routeInfo);
		this.ttl = ttl;
	}
	
	public static RouteInfo build(String routeInfo) {
		return build(routeInfo, 1);
	}
	
	public static RouteInfo build(String routeInfo, int ttl) {
		return new RouteInfo(routeInfo, ttl);
	}
	
	/**
	 * 添加到路由链表中
	 * @param routeInfo
	 */
	public void addRouteInfo(String routeInfo) {
		routelist.add(routeInfo);
	}
	
	/**
	 * 判断是否已在路由链表中
	 * @param routeInfo
	 * @return
	 */
	public boolean isInRoute(String routeInfo) {
		int index = routelist.indexOf(routeInfo);
		return index != -1 ? true : false;
	}
	
	/**
	 * 返回当前跳数
	 * @return
	 */
	public int ttl() {
		return ttl;
	}
	
	/**
	 * 递减跳数，返回递减后的跳数
	 * @return
	 */
	public int decreasementTTL() {
		return --ttl;
	}
	
	/**
	 * 打印路由信息
	 * @return
	 */
	public String printRouteInfo() {
		if (routelist.size() == 0)
			return "no route";
		
		StringBuilder sb = new StringBuilder();
		
		Iterator<String> iterator = routelist.iterator();
		while (iterator.hasNext()) {
			sb.append("->"+iterator.next());
		}
		
		return sb.toString();
	}
}
