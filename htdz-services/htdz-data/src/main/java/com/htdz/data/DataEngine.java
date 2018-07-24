package com.htdz.data;


import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.htdz.common.LogManager;
import com.htdz.db.service.UserService;
import com.htdz.def.dbmodel.User;


@Service
public class DataEngine {
	@Autowired
	private UserService userService;
	
	public void onContextInitCompleted() {
		LogManager.info("----------容器初始化完成----------");
		
//		List<User> users = userService.selectAllUser();
//		if (users != null && users.size() > 0) {
//			for (User user: users)
//				System.out.println(user);
//		}
		User user = userService.selectUserBySql("select * from user where id=1");
		if (user != null)
			System.out.println(user);
	}
}
