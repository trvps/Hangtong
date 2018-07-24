package io.rong;

import io.rong.messages.TxtMessage;
import io.rong.models.ChatRoomInfo;
import io.rong.models.ChatroomQueryResult;
import io.rong.models.ChatroomUserQueryResult;
import io.rong.models.CodeSuccessResult;
import io.rong.models.GroupInfo;
import io.rong.models.GroupUserQueryResult;
import io.rong.models.ListBlockChatroomUserResult;
import io.rong.models.ListGagChatroomUserResult;
import io.rong.models.ListGagGroupUserResult;
import io.rong.models.SMSImageCodeResult;
import io.rong.models.SMSSendCodeResult;
import io.rong.models.SMSVerifyCodeResult;
import io.rong.models.TokenResult;

import java.io.Reader;

/**
 * 一些api的调用示例
 */
public class Example {
	//private static final String JSONFILE = Example.class.getClassLoader().getResource("jsonsource").getPath()+"/";
	/**
	 * 本地调用测试
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		String appKey = "p5tvi9dsp40o4";//替换成您的appkey
		String appSecret = "H3OAxrxtEH";//替换成匹配上面key的secret
		
		Reader reader = null ;
		RongCloud rongCloud = RongCloud.getInstance(appKey, appSecret);
				
		
		System.out.println("************************User********************");
		// 获取 Token 方法 
		TokenResult userGetTokenResult = rongCloud.user.getToken("userId", "userName", "http://www.rongcloud.cn/images/logo.png");
		System.out.println("getToken:  " + userGetTokenResult.toString());

	
		
		// 发送群组消息方法（以一个用户身份向群组发送消息，单条消息最大 128k.每秒钟最多发送 20 条消息，每次最多向 3 个群组发送，如：一次向 3 个群组发送消息，示为 3 条消息。） 
		String[] messagePublishGroupToGroupId = {"groupId1","groupId2","groupId3"};
		TxtMessage messagePublishGroupTxtMessage = new TxtMessage("hello", "helloExtra");
		CodeSuccessResult messagePublishGroupResult = rongCloud.message.publishGroup("userId", messagePublishGroupToGroupId, messagePublishGroupTxtMessage, "thisisapush", "{\"pushData\":\"hello\"}", 1, 1, 0);
		System.out.println("publishGroup:  " + messagePublishGroupResult.toString());
		
		// 发送讨论组消息方法（以一个用户身份向讨论组发送消息，单条消息最大 128k，每秒钟最多发送 20 条消息.） 
		TxtMessage messagePublishDiscussionTxtMessage = new TxtMessage("hello", "helloExtra");
		CodeSuccessResult messagePublishDiscussionResult = rongCloud.message.publishDiscussion("userId1", "discussionId1", messagePublishDiscussionTxtMessage, "thisisapush", "{\"pushData\":\"hello\"}", 1, 1, 0);
		System.out.println("publishDiscussion:  " + messagePublishDiscussionResult.toString());
		
		// 发送聊天室消息方法（一个用户向聊天室发送消息，单条消息最大 128k。每秒钟限 100 次。） 
		String[] messagePublishChatroomToChatroomId = {"ChatroomId1","ChatroomId2","ChatroomId3"};
		TxtMessage messagePublishChatroomTxtMessage = new TxtMessage("hello", "helloExtra");
		CodeSuccessResult messagePublishChatroomResult = rongCloud.message.publishChatroom("userId1", messagePublishChatroomToChatroomId, messagePublishChatroomTxtMessage);
		System.out.println("publishChatroom:  " + messagePublishChatroomResult.toString());
		
		
	
		
		System.out.println("************************Group********************");
		// 创建群组方法（创建群组，并将用户加入该群组，用户将可以收到该群的消息，同一用户最多可加入 500 个群，每个群最大至 3000 人，App 内的群组数量没有限制.注：其实本方法是加入群组方法 /group/join 的别名。） 
		String[] groupCreateUserId = {"userId1","userid2","userId3"};
		CodeSuccessResult groupCreateResult = rongCloud.group.create(groupCreateUserId, "groupId1", "groupName1");
		System.out.println("create:  " + groupCreateResult.toString());
		
		// 同步用户所属群组方法（当第一次连接融云服务器时，需要向融云服务器提交 userId 对应的用户当前所加入的所有群组，此接口主要为防止应用中用户群信息同融云已知的用户所属群信息不同步。） 
		GroupInfo[] groupSyncGroupInfo = {new GroupInfo("groupId1","groupName1" ), new GroupInfo("groupId2","groupName2" ), new GroupInfo("groupId3","groupName3" )};
		CodeSuccessResult groupSyncResult = rongCloud.group.sync("userId1", groupSyncGroupInfo);
		System.out.println("sync:  " + groupSyncResult.toString());
		
		// 刷新群组信息方法 
		CodeSuccessResult groupRefreshResult = rongCloud.group.refresh("groupId1", "newGroupName");
		System.out.println("refresh:  " + groupRefreshResult.toString());
		
		// 将用户加入指定群组，用户将可以收到该群的消息，同一用户最多可加入 500 个群，每个群最大至 3000 人。 
		String[] groupJoinUserId = {"userId2","userid3","userId4"};
		CodeSuccessResult groupJoinResult = rongCloud.group.join(groupJoinUserId, "groupId1", "TestGroup");
		System.out.println("join:  " + groupJoinResult.toString());
		
		// 查询群成员方法 
		GroupUserQueryResult groupQueryUserResult = rongCloud.group.queryUser("groupId1");
		System.out.println("queryUser:  " + groupQueryUserResult.toString());
		
		// 退出群组方法（将用户从群中移除，不再接收该群组的消息.） 
		String[] groupQuitUserId = {"userId2","userid3","userId4"};
		CodeSuccessResult groupQuitResult = rongCloud.group.quit(groupQuitUserId, "TestGroup");
		System.out.println("quit:  " + groupQuitResult.toString());
		
		// 添加禁言群成员方法（在 App 中如果不想让某一用户在群中发言时，可将此用户在群组中禁言，被禁言用户可以接收查看群组中用户聊天信息，但不能发送消息。） 
		CodeSuccessResult groupAddGagUserResult = rongCloud.group.addGagUser("userId1", "groupId1", "1");
		System.out.println("addGagUser:  " + groupAddGagUserResult.toString());
		
		// 查询被禁言群成员方法 
		ListGagGroupUserResult groupLisGagUserResult = rongCloud.group.lisGagUser("groupId1");
		System.out.println("lisGagUser:  " + groupLisGagUserResult.toString());
		
		// 移除禁言群成员方法 
		String[] groupRollBackGagUserUserId = {"userId2","userid3","userId4"};
		CodeSuccessResult groupRollBackGagUserResult = rongCloud.group.rollBackGagUser(groupRollBackGagUserUserId, "groupId1");
		System.out.println("rollBackGagUser:  " + groupRollBackGagUserResult.toString());
		
		// 解散群组方法。（将该群解散，所有用户都无法再接收该群的消息。） 
		CodeSuccessResult groupDismissResult = rongCloud.group.dismiss("userId1", "groupId1");
		System.out.println("dismiss:  " + groupDismissResult.toString());
		
		
		
		System.out.println("************************Chatroom********************");
		// 创建聊天室方法 
		ChatRoomInfo[] chatroomCreateChatRoomInfo = {new ChatRoomInfo("chatroomId1","chatroomName1" ), new ChatRoomInfo("chatroomId2","chatroomName2" ), new ChatRoomInfo("chatroomId3","chatroomName3" )};
		CodeSuccessResult chatroomCreateResult = rongCloud.chatroom.create(chatroomCreateChatRoomInfo);
		System.out.println("create:  " + chatroomCreateResult.toString());
		
		// 加入聊天室方法 
		String[] chatroomJoinUserId = {"userId2","userid3","userId4"};
		CodeSuccessResult chatroomJoinResult = rongCloud.chatroom.join(chatroomJoinUserId, "chatroomId1");
		System.out.println("join:  " + chatroomJoinResult.toString());
		
		// 查询聊天室信息方法 
		String[] chatroomQueryChatroomId = {"chatroomId1","chatroomId2","chatroomId3"};
		ChatroomQueryResult chatroomQueryResult = rongCloud.chatroom.query(chatroomQueryChatroomId);
		System.out.println("query:  " + chatroomQueryResult.toString());
		
		// 查询聊天室内用户方法 
		ChatroomUserQueryResult chatroomQueryUserResult = rongCloud.chatroom.queryUser("chatroomId1", "500", "2");
		System.out.println("queryUser:  " + chatroomQueryUserResult.toString());
		
		// 聊天室消息停止分发方法（可实现控制对聊天室中消息是否进行分发，停止分发后聊天室中用户发送的消息，融云服务端不会再将消息发送给聊天室中其他用户。） 
		CodeSuccessResult chatroomStopDistributionMessageResult = rongCloud.chatroom.stopDistributionMessage("chatroomId1");
		System.out.println("stopDistributionMessage:  " + chatroomStopDistributionMessageResult.toString());
		
		// 聊天室消息恢复分发方法 
		CodeSuccessResult chatroomResumeDistributionMessageResult = rongCloud.chatroom.resumeDistributionMessage("chatroomId1");
		System.out.println("resumeDistributionMessage:  " + chatroomResumeDistributionMessageResult.toString());
		
		// 添加禁言聊天室成员方法（在 App 中如果不想让某一用户在聊天室中发言时，可将此用户在聊天室中禁言，被禁言用户可以接收查看聊天室中用户聊天信息，但不能发送消息.） 
		CodeSuccessResult chatroomAddGagUserResult = rongCloud.chatroom.addGagUser("userId1", "chatroomId1", "1");
		System.out.println("addGagUser:  " + chatroomAddGagUserResult.toString());
		
		// 查询被禁言聊天室成员方法 
		ListGagChatroomUserResult chatroomListGagUserResult = rongCloud.chatroom.ListGagUser("chatroomId1");
		System.out.println("ListGagUser:  " + chatroomListGagUserResult.toString());
		
		// 移除禁言聊天室成员方法 
		CodeSuccessResult chatroomRollbackGagUserResult = rongCloud.chatroom.rollbackGagUser("userId1", "chatroomId1");
		System.out.println("rollbackGagUser:  " + chatroomRollbackGagUserResult.toString());
		
		// 添加封禁聊天室成员方法 
		CodeSuccessResult chatroomAddBlockUserResult = rongCloud.chatroom.addBlockUser("userId1", "chatroomId1", "1");
		System.out.println("addBlockUser:  " + chatroomAddBlockUserResult.toString());
		
		// 查询被封禁聊天室成员方法 
		ListBlockChatroomUserResult chatroomGetListBlockUserResult = rongCloud.chatroom.getListBlockUser("chatroomId1");
		System.out.println("getListBlockUser:  " + chatroomGetListBlockUserResult.toString());
		
		// 移除封禁聊天室成员方法 
		CodeSuccessResult chatroomRollbackBlockUserResult = rongCloud.chatroom.rollbackBlockUser("userId1", "chatroomId1");
		System.out.println("rollbackBlockUser:  " + chatroomRollbackBlockUserResult.toString());
		
		// 添加聊天室消息优先级方法 
		String[] chatroomAddPriorityObjectName = {"RC:VcMsg","RC:ImgTextMsg","RC:ImgMsg"};
		CodeSuccessResult chatroomAddPriorityResult = rongCloud.chatroom.addPriority(chatroomAddPriorityObjectName);
		System.out.println("addPriority:  " + chatroomAddPriorityResult.toString());
		
		// 销毁聊天室方法 
		String[] chatroomDestroyChatroomId = {"chatroomId","chatroomId1","chatroomId2"};
		CodeSuccessResult chatroomDestroyResult = rongCloud.chatroom.destroy(chatroomDestroyChatroomId);
		System.out.println("destroy:  " + chatroomDestroyResult.toString());
		
		// 添加聊天室白名单成员方法 
		String[] chatroomAddWhiteListUserUserId = {"userId1","userId2","userId3","userId4","userId5"};
		CodeSuccessResult chatroomAddWhiteListUserResult = rongCloud.chatroom.addWhiteListUser("chatroomId", chatroomAddWhiteListUserUserId);
		System.out.println("addWhiteListUser:  " + chatroomAddWhiteListUserResult.toString());
		
		
		

		
		
		System.out.println("************************SMS********************");
		// 获取图片验证码方法 
		SMSImageCodeResult sMSGetImageCodeResult = rongCloud.sms.getImageCode("app-key");
		System.out.println("getImageCode:  " + sMSGetImageCodeResult.toString());
		
		// 发送短信验证码方法。 
		SMSSendCodeResult sMSSendCodeResult = rongCloud.sms.sendCode("13500000000", "dsfdsfd", "86", "1408706337", "1408706337");
		System.out.println("sendCode:  " + sMSSendCodeResult.toString());
		
		// 验证码验证方法 
		SMSVerifyCodeResult sMSVerifyCodeResult = rongCloud.sms.verifyCode("2312312", "2312312");
		System.out.println("verifyCode:  " + sMSVerifyCodeResult.toString());
		
	 }
}