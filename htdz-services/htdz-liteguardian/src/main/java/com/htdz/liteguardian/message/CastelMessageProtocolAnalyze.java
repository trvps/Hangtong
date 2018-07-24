package com.htdz.liteguardian.message;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
/**
 * 
 * 协议解析
 * @author user
 *
 */
public class CastelMessageProtocolAnalyze
{
  public byte[] msgByte;
  public List<Byte> msgExcludeFlagConvert;
  public Byte[] msgHeadBody;
  public Byte[] msgHead;
  public Byte[] msgBody;
  public Byte checkCode;
  public final byte flag = 0x7e;
  Logger log = Logger.getLogger(this.getClass());

  public boolean checkStatus = false;
  
  public CastelMessageProtocolAnalyze(byte[] msgbyte)
  {
	    this.msgByte = msgbyte;
	    InitByteArray();
  }

  //验证数据格式是否正确
  public boolean CheckMsgIsIntact()
  {
    if ((this.msgByte == null) || (this.msgByte.length < 31)) {
      return false;
    }

    if ((this.msgByte[0] != flag) || (this.msgByte[(this.msgByte.length - 1)] != flag)) {
      return false;
    }

    return true;
  }

  //初始化消息各部分BYTE数组
  private void InitByteArray()
  {
    if (CheckMsgIsIntact())
      try {
        this.msgExcludeFlagConvert = new ArrayList<Byte>();

        for (int i = 1; i <= this.msgByte.length - 2; i++) {
          if (this.msgByte[i] != (byte)0x7d)
          {
            this.msgExcludeFlagConvert.add(this.msgByte[i]);
          }
          else if (this.msgByte[(i + 1)] == (byte)0x01)
          {
            this.msgExcludeFlagConvert.add((byte)0x7d);
            i++;
          }
          else if (this.msgByte[(i + 1)] == (byte)0x02) 
          {
            this.msgExcludeFlagConvert.add((byte)0x7e);
            i++;
          }
        }

        this.checkCode = ((Byte)this.msgExcludeFlagConvert.get(this.msgExcludeFlagConvert.size() - 1));

        this.msgHeadBody = new Byte[this.msgExcludeFlagConvert.size() - 1];
        this.msgExcludeFlagConvert.subList(0, 
          this.msgExcludeFlagConvert.size() - 1).toArray(this.msgHeadBody);

        this.msgHead = new Byte[28];
        this.msgExcludeFlagConvert.subList(0, 28).toArray(this.msgHead);

        this.msgBody = new Byte[this.msgExcludeFlagConvert.size() - 29];
        if (this.msgExcludeFlagConvert.size() - 2 >= 28)
          this.msgExcludeFlagConvert.subList(28, 
            this.msgExcludeFlagConvert.size() - 1).toArray(this.msgBody);
      }
      catch (Exception e) {
        this.log.debug(e.getMessage());
      }
  }

  //检查验证码是否正确
  public boolean CkeckCodeIsCorrect()
  {
    try
    {
      if ((this.msgHeadBody != null) && (this.msgHeadBody.length >= 28)) 
      {
        Byte curCheckCode = this.msgHeadBody[0];
        for (int i = 1; i <= this.msgHeadBody.length - 1; i++) 
        {
          curCheckCode = (byte)(curCheckCode.byteValue() ^ this.msgHeadBody[i].byteValue());
        }

        this.checkStatus = (curCheckCode.byteValue() == this.checkCode.byteValue());
      } 
      else 
      {
        this.checkStatus = false;
      }
    } catch (Exception e) {
      this.log.debug(e.getMessage());

      this.checkStatus = false;
    }

    return this.checkStatus;
  }

  //获取消息头部消息ID
  public short getMsgHeadId()
  {
    if ((this.checkStatus) && (this.msgHead != null)) {
      try {
        return byte2short(new byte[] { this.msgHead[0].byteValue(), this.msgHead[1].byteValue() });
      } catch (Exception e) {
        this.log.debug(e.getMessage());
        return 0;
      }
    }
    return 0;
  }
  
 //获取消息头部消息ID的静态方法
  public static short getMsgHeadId(byte[] msgByte)
  {
	 try
	 {
	     return (short)(msgByte[2] & 0xFF | (msgByte[1] & 0xFF) << 8);
	 } 
	 catch (Exception e) 
	 {
	    return 0;
	 }
  }

  //获取消息头部消息长度
  public short getMsgHeadLength()
  {
    if ((this.checkStatus) && (this.msgHead != null)) {
      try {
        return byte2short(new byte[] { this.msgHead[2].byteValue(), this.msgHead[3].byteValue() });
      } catch (Exception e) {
        this.log.debug(e.getMessage());
        return 0;
      }
    }
    return 0;
  }

  //获取消息头部协议版本
  public short getMsgHeadProtocol()
  {
    if ((this.checkStatus) && (this.msgHead != null)) {
      try {
        return byte2short(new byte[] { this.msgHead[4].byteValue(), this.msgHead[5].byteValue() });
      } catch (Exception e) {
        this.log.debug(e.getMessage());
        return 0;
      }
    }
    return 0;
  }

  //获取消息头部设备号码
  public String getMsgHeadEquipId()
  {
    if ((this.checkStatus) && (this.msgHead != null)) {
      try {
        byte[] equipId = new byte[20];

        int i = 0;
        while (i < 20) {
          if (this.msgHead[(i + 6)].byteValue() == (byte)0) 
          {
        	  break;
          }
          equipId[i] = this.msgHead[(i + 6)].byteValue();
          i++;
        }

        if (i == 0) {
          return "";
        }
        return new String(equipId, 0, i);
      }
      catch (Exception e) {
        this.log.debug(e.getMessage());
        return "";
      }
    }
    return "";
  }

  //获取消息头部序列号
  public short getMsgHeadSequice()
  {
    if ((this.checkStatus) && (this.msgHead != null)) {
      try {
        return byte2short(new byte[] { this.msgHead[26].byteValue(), this.msgHead[27].byteValue() });
      } catch (Exception e) {
        this.log.debug(e.getMessage());
        return 0;
      }
    }
    return 0;
  }

  //获取消息头部对象
  public CastelMessageHeadVO getMsgHeadObj()
  {
    CastelMessageHeadVO msgHeadVo = new CastelMessageHeadVO();
    
    msgHeadVo.setMsgId(getMsgHeadId());
    msgHeadVo.setProtocolType(getMsgHeadProtocol());
    msgHeadVo.setEquipId(getMsgHeadEquipId());
    msgHeadVo.setMsgSquice(getMsgHeadSequice());

    return msgHeadVo;
  }

  
  //获取消息体消息String内容
  public String msgValue2String(byte[] msgValue)
  {
    if ((msgValue != null) && (msgValue.length >= 1)) 
    {
      try
      {
        return new String(msgValue);
      } 
      catch (Exception e) 
      {
        this.log.debug(e.getMessage());
        return "";
      }
    }
    
    return "";
  }
  
  //获取消息体消息Char内容
  @SuppressWarnings("unused")
  public String msgValue2StringByChar(byte[] msgValue)
  {
    if ((msgValue != null) && (msgValue.length >= 1)) 
    {
      try
      {
    	  if(msgValue.length==1 && msgValue[0]==(byte)0)
    	  {
    		  return "";
    	  }
    	  
    	  Charset cs = Charset.defaultCharset();
          ByteBuffer byteBuffer = ByteBuffer.allocate(msgValue.length);
          
          byteBuffer.put(msgValue);
          byteBuffer.flip();
          
          CharBuffer cb = cs.decode(byteBuffer);
          
    	  return new String(cb.array());
      } 
      catch (Exception e) 
      {
        this.log.debug(e.getMessage());
        return "";
      }
    }
    
    return "";
  }

  /**
   * byte转16位
   * @param b
   * @return
   */
  public short byte2short(byte[] b)
  {
	  //大端
    return (short)(b[1] & 0xFF | (b[0] & 0xFF) << 8);
  }

  /**
   * byte转32位
   * @param b
   * @return
   */
  public int byte2int(byte[] b)
  {
    return b[3] & 0xFF | (b[2] & 0xFF) << 8 | (b[1] & 0xFF) << 16 |  (b[0] & 0xFF) << 24;
  }
}
