package com.uni.cloud.chat.misc;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

import android.util.Log;

public class ByteUtil {
	private static final String TAG = "ByteUtil";
	public static byte[] getBytes(short data) {
		byte[] bytes = new byte[2];
		bytes[0] = (byte) ((data & 0xff00) >> 8);
		bytes[1] = (byte) (data & 0xff);
		return bytes;
	}

	public static byte[] getBytes(char data) {
		byte[] bytes = new byte[2];
		bytes[0] = (byte) (data >> 8);
		bytes[1] = (byte) (data);
		return bytes;
	}

	public static byte[] getBytes(int data) {
		byte[] bytes = new byte[4];
		bytes[3] = (byte) (data & 0xff);
		bytes[2] = (byte) ((data & 0xff00) >> 8);
		bytes[1] = (byte) ((data & 0xff0000) >> 16);
		bytes[0] = (byte) ((data & 0xff000000) >> 24);
		return bytes;
	}

	public static byte[] getBytes(long data) {
		byte[] bytes = new byte[8];
		bytes[7] = (byte) (data & 0xff);
		bytes[6] = (byte) ((data >> 8) & 0xff);
		bytes[5] = (byte) ((data >> 16) & 0xff);
		bytes[4] = (byte) ((data >> 24) & 0xff);
		bytes[3] = (byte) ((data >> 32) & 0xff);
		bytes[2] = (byte) ((data >> 40) & 0xff);
		bytes[1] = (byte) ((data >> 48) & 0xff);
		bytes[0] = (byte) ((data >> 56) & 0xff);
		Log.d(TAG, "getBytes=="+byte2hex(bytes));
		return bytes;
	}

	public static byte[] getBytes(float data) {
		int intBits = Float.floatToIntBits(data);
		return getBytes(intBits);
	}

	public static byte[] getBytes(double data) {
		long intBits = Double.doubleToLongBits(data);
		Log.d(TAG, "double data=="+data);
		Log.d(TAG, "intBits =="+ intBits);
		return getBytes(intBits);
	}

	public static byte[] getBytes(String data, String charsetName) {
		Charset charset = Charset.forName(charsetName);
		return data.getBytes(charset);
	}

	public static byte[] getBytes(String data) {
		return getBytes(data, "UTF-8");
//		return getBytes(data, "GBK");	
	}

	public static short getShort(byte[] bytes) {
		return (short) ((0xff & bytes[1])
				| (0xff00 & (bytes[0] << 8)));
	}

	public static char getChar(byte[] bytes) {
		return (char) ((0xff & bytes[1])
				| (0xff00 & (bytes[0] << 8)));
	}

	public static int getInt(byte[] bytes) {
		return (0xff & bytes[3])
				| (0xff00 & (bytes[2] << 8))
				| (0xff0000 & (bytes[1] << 16))
				| (0xff000000 & (bytes[0] << 24));
	}

	public static long getLong(byte[] bytes) {
		return (0xffL & (long) bytes[7])
				| (0xff00L & ((long) bytes[6] << 8))
				| (0xff0000L & ((long) bytes[5] << 16))
				| (0xff000000L & ((long) bytes[4] << 24))
				| (0xff00000000L & ((long) bytes[3] << 32))
				| (0xff0000000000L & ((long) bytes[2] << 40))
				| (0xff000000000000L & ((long) bytes[1] << 48))
				| (0xff00000000000000L & ((long) bytes[0] << 56));
	}


	/**
	 *用户id,6字节
	 * @param data
	 * @return
	 */
	public static byte[] getBytesUserId(long data) {
		byte[] bytes = new byte[6];
		bytes[5] = (byte) (data & 0xff);
		bytes[4] = (byte) ((data >> 8) & 0xff);
		bytes[3] = (byte) ((data >> 16) & 0xff);
		bytes[2] = (byte) ((data >> 24) & 0xff);
		bytes[1] = (byte) ((data >> 32) & 0xff);
		bytes[0] = (byte) ((data >> 40) & 0xff);
		/*bytes[1] = (byte) ((data >> 48) & 0xff);
		bytes[0] = (byte) ((data >> 56) & 0xff);*/
		Log.d(TAG, "getBytes=="+byte2hex(bytes));
		return bytes;
	}

	/**
	 * 用户ID，6字节
	 * @param bytes
	 * @return
	 */
	public static long getUserId(byte[] bytes) {
		return (0xffL & (long) bytes[5])
				| (0xff00L & ((long) bytes[4] << 8))
				| (0xff0000L & ((long) bytes[3] << 16))
				| (0xff000000L & ((long) bytes[2] << 24))
				| (0xff00000000L & ((long) bytes[1] << 32))
				| (0xff0000000000L & ((long) bytes[0] << 40));
	}

	public static float getFloat(byte[] bytes) {
		return Float.intBitsToFloat(getInt(bytes));
	}

	public static double getDouble(byte[] bytes) {
		long l = getLong(bytes);
		System.out.println(l);
		return Double.longBitsToDouble(l);
	}

	public static String getString(byte[] bytes, String charsetName) {
		return new String(bytes, Charset.forName(charsetName));
	}

	public static String getString(byte[] bytes) {
//		return getString(bytes, "GBK");
		return getString(bytes, "UTF-8");
	}

	public static String byte2hex(byte[] buffer) {
		String h = "";

		for (int i = 0; i < buffer.length; i++) {
			String temp = Integer.toHexString(buffer[i] & 0xFF);
			if (temp.length() == 1) {
				temp = "0" + temp;
			}
			h = h + " " + temp;
		}

		return h;

	}

	public static byte[] mac_string2byte(String mac_str){
		byte[] bytes = new byte[6];

		if(mac_str.length()==17)
		{
			String[] strr = stringSplit0(mac_str);
			if(strr.length == 6)
			{
				for(int i=0; i < 6; i++)
				{
					Log.d("APPLICATION_TAG", "hexStr2Bytes byte ="+hexStr2Bytes(strr[i])[0]);
					Log.d("APPLICATION_TAG", "hexStr2Bytes(strr[i]).length ="+hexStr2Bytes(strr[i]).length);
					bytes[i] = hexStr2Bytes(strr[i])[0];
					Log.d("APPLICATION_TAG", "bytes[i]  ="+bytes[i] );

				}
			}
		}

		return bytes;

	}

	public static String[] stringSplit0(String str) {
		String[] sourceStrArray = str.split(":");
		// for (int i = 0; i < sourceStrArray.length; i++) {
		// Log.d(TAGZK, "sourceStrArray[i] = " + sourceStrArray[i]);
		// }
		return sourceStrArray;
	}

	/**
	 * 字符串转换成十六进制字符串
	 */

	public static String str2HexStr(String str) {

		char[] chars = "0123456789ABCDEF".toCharArray();
		StringBuilder sb = new StringBuilder("");
		byte[] bs = str.getBytes();
		int bit;
		for (int i = 0; i < bs.length; i++) {
			bit = (bs[i] & 0x0f0) >> 4;
			sb.append(chars[bit]);
			bit = bs[i] & 0x0f;
			sb.append(chars[bit]);
		}
		return sb.toString();
	}

	/**
	 *
	 * 十六进制转换字符串
	 */

	public static String hexStr2Str(String hexStr) {
		String str = "0123456789ABCDEF";
		char[] hexs = hexStr.toCharArray();
		byte[] bytes = new byte[hexStr.length() / 2];
		int n;
		for (int i = 0; i < bytes.length; i++) {
			n = str.indexOf(hexs[2 * i]) * 16;
			n += str.indexOf(hexs[2 * i + 1]);
			bytes[i] = (byte) (n & 0xff);
		}
		return new String(bytes);
	}

	/**
	 * bytes转换成十六进制字符串
	 */
	public static String byte2HexStr(byte[] b) {
		String hs = "";
		String stmp = "";
		for (int n = 0; n < b.length; n++) {
			stmp = (Integer.toHexString(b[n] & 0XFF));
			if (stmp.length() == 1)
				hs = hs + "0" + stmp;
			else
				hs = hs + stmp;
			// if (n<b.length-1) hs=hs+":";
		}
		return hs.toUpperCase();
	}

	private static byte uniteBytes(String src0, String src1) {
		byte b0 = Byte.decode("0x" + src0).byteValue();
		b0 = (byte) (b0 << 4);
		byte b1 = Byte.decode("0x" + src1).byteValue();
		byte ret = (byte) (b0 | b1);
		return ret;
	}

	/**
	 * bytes转换成十六进制字符串
	 */
	public static byte[] hexStr2Bytes(String src) {
		int m = 0, n = 0;
		int l = src.length() / 2;
		System.out.println(l);
		byte[] ret = new byte[l];
		for (int i = 0; i < l; i++) {
			m = i * 2 + 1;
			n = m + 1;
			ret[i] = uniteBytes(src.substring(i * 2, m), src.substring(m, n));
		}
		return ret;
	}

	/**
	 * String的字符串转换成unicode的String
	 */
	public static String str2Unicode(String strText) throws Exception {
		char c;
		String strRet = "";
		int intAsc;
		String strHex;
		for (int i = 0; i < strText.length(); i++) {
			c = strText.charAt(i);
			intAsc = (int) c;
			strHex = Integer.toHexString(intAsc);
			if (intAsc > 128) {
				strRet += "//u" + strHex;
			} else {
				// 低位在前面补00
				strRet += "//u00" + strHex;
			}
		}
		return strRet;
	}

	/**
	 * unicode的String转换成String的字符串
	 */
	public static String unicode2Str(String hex) {
		int t = hex.length() / 6;
		StringBuilder str = new StringBuilder();
		for (int i = 0; i < t; i++) {
			String s = hex.substring(i * 6, (i + 1) * 6);
			// 高位需要补上00再转
			String s1 = s.substring(2, 4) + "00";
			// 低位直接转
			String s2 = s.substring(4);
			// 将16进制的string转为int
			int n = Integer.valueOf(s1, 16) + Integer.valueOf(s2, 16);
			// 将int转换为字符
			char[] chars = Character.toChars(n);
			str.append(new String(chars));
		}
		return str.toString();
	}

	/**
	 *
	 * 从指定数组的copy一个子数组并返回
	 *
	 *
	 *
	 * @param org
	 *            of type byte[] 原数组
	 *
	 * @param to
	 *            合并一个byte[]
	 *
	 * @return 合并的数据
	 */
	public static byte[] append(byte[] org, byte[] to) {

		byte[] newByte = new byte[org.length + to.length];

		System.arraycopy(org, 0, newByte, 0, org.length);

		System.arraycopy(to, 0, newByte, org.length, to.length);

		return newByte;

	}

	/**

	 * 从指定数组的copy一个子数组并返回

	 *

	 * @param org of type byte[] 原数组

	 * @param to 合并一个byte

	 * @return 合并的数据

	 */

	public static byte[] append(byte[] org, byte to) {

		byte[] newByte = new byte[org.length + 1];

		System.arraycopy(org, 0, newByte, 0, org.length);

		newByte[org.length] = to;

		return newByte;

	}


	/**
	 *
	 * 从指定数组的copy一个子数组并返回
	 *
	 *
	 *
	 * @param org
	 *            of type byte[] 原数组
	 *
	 * @param from
	 *            起始点
	 *
	 * @param append
	 *            要合并的数据
	 */

	public static void append(byte[] org, int from, byte[] append) {

		System.arraycopy(append, 0, org, from, append.length);

	}

	/**
	 *
	 * 从指定数组的copy一个子数组并返回
	 *
	 *
	 *
	 * @param original
	 *            of type byte[] 原数组
	 *
	 * @param from
	 *            起始点
	 *
	 * @param to
	 *            结束点
	 *
	 * @return 返回copy的数组
	 */

	public static byte[] copyOfRange(byte[] original, int from, int to) {

		int newLength = to - from;

		if (newLength < 0)
			return null;

		byte[] copy = new byte[newLength];

		System.arraycopy(original, from, copy, 0,

				Math.min(original.length - from, newLength));

		return copy;

	}

	public static byte[] char2byte(String encode, char... chars) {

		Charset cs = Charset.forName(encode);

		CharBuffer cb = CharBuffer.allocate(chars.length);

		cb.put(chars);

		cb.flip();

		ByteBuffer bb = cs.encode(cb);

		return bb.array();

	}

	/**
	 *
	 * 查找并替换指定byte数组
	 *
	 *
	 *
	 * @param org
	 *            of type byte[] 原数组
	 *
	 * @param search
	 *            of type byte[] 要查找的数组
	 *
	 * @param replace
	 *            of type byte[] 要替换的数组
	 *
	 * @param startIndex
	 *            of type int 开始搜索索引
	 *
	 * @return byte[] 返回新的数组
	 *
	 * @throws UnsupportedEncodingException
	 *             when
	 */

	public static byte[] arrayReplace(byte[] org, byte[] search,
									  byte[] replace, int startIndex) throws UnsupportedEncodingException {

		int index = indexOf(org, search, startIndex);

		if (index != -1) {

			int newLength = org.length + replace.length - search.length;

			byte[] newByte = new byte[newLength];

			System.arraycopy(org, 0, newByte, 0, index);

			System.arraycopy(replace, 0, newByte, index, replace.length);

			System.arraycopy(org, index + search.length, newByte, index
					+ replace.length, org.length - index - search.length);

			int newStart = index + replace.length;

			// String newstr = new String(newByte, “GBK”);

			// System.out.println(newstr);

			if ((newByte.length - newStart) > replace.length) {

				return arrayReplace(newByte, search, replace, newStart);

			}

			return newByte;

		} else {

			return org;

		}

	}

	/**
	 *
	 * 查找指定数组的起始索引
	 *
	 *
	 *
	 * @param org
	 *            of type byte[] 原数组
	 *
	 * @param search
	 *            of type byte[] 要查找的数组
	 *
	 * @return int 返回索引
	 */

	public static int indexOf(byte[] org, byte[] search) {

		return indexOf(org, search, 0);

	}

	/**
	 *
	 * 查找指定数组的起始索引
	 *
	 *
	 *
	 * @param org
	 *            of type byte[] 原数组
	 *
	 * @param search
	 *            of type byte[] 要查找的数组
	 *
	 * @param startIndex
	 *            起始索引
	 *
	 * @return int 返回索引
	 */

	public static int indexOf(byte[] org, byte[] search, int startIndex) {

		KMPMatcher kmpMatcher = new KMPMatcher();

		kmpMatcher.computeFailure4Byte(search);

		return kmpMatcher.indexOf(org, startIndex);

		// return com.alibaba.common.lang.ArrayUtil.indexOf(org, search);

	}

	/**
	 *
	 * 查找指定数组的最后一次出现起始索引
	 *
	 *
	 *
	 * @param org
	 *            of type byte[] 原数组
	 *
	 * @param search
	 *            of type byte[] 要查找的数组
	 *
	 * @return int 返回索引
	 */

	public static int lastIndexOf(byte[] org, byte[] search) {

		return lastIndexOf(org, search, 0);

	}

	/**
	 *
	 * 查找指定数组的最后一次出现起始索引
	 *
	 *
	 *
	 * @param org
	 *            of type byte[] 原数组
	 *
	 * @param search
	 *            of type byte[] 要查找的数组
	 *
	 * @param fromIndex
	 *            起始索引
	 *
	 * @return int 返回索引
	 */

	public static int lastIndexOf(byte[] org, byte[] search, int fromIndex) {

		KMPMatcher kmpMatcher = new KMPMatcher();

		kmpMatcher.computeFailure4Byte(search);

		return kmpMatcher.lastIndexOf(org, fromIndex);

	}

	/**
	 *
	 * KMP算法类
	 *
	 * <p/>
	 *
	 * Created on 2011-1-3
	 */

	static class KMPMatcher {

		private int[] failure;

		private int matchPoint;

		private byte[] bytePattern;

		/**
		 *
		 * Method indexOf …
		 *
		 *
		 *
		 * @param text
		 *            of type byte[]
		 *
		 * @param startIndex
		 *            of type int
		 *
		 * @return int
		 */

		public int indexOf(byte[] text, int startIndex) {

			int j = 0;

			if (text.length == 0 || startIndex > text.length)
				return -1;

			for (int i = startIndex; i < text.length; i++) {

				while (j > 0 && bytePattern[j] != text[i]) {

					j = failure[j - 1];

				}

				if (bytePattern[j] == text[i]) {

					j++;

				}

				if (j == bytePattern.length) {

					matchPoint = i - bytePattern.length + 1;

					return matchPoint;

				}

			}

			return -1;

		}

		/**
		 *
		 * 找到末尾后重头开始找
		 *
		 *
		 *
		 * @param text
		 *            of type byte[]
		 *
		 * @param startIndex
		 *            of type int
		 *
		 * @return int
		 */

		public int lastIndexOf(byte[] text, int startIndex) {

			matchPoint = -1;

			int j = 0;

			if (text.length == 0 || startIndex > text.length)
				return -1;

			int end = text.length;

			for (int i = startIndex; i < end; i++) {

				while (j > 0 && bytePattern[j] != text[i]) {

					j = failure[j - 1];

				}

				if (bytePattern[j] == text[i]) {

					j++;

				}

				if (j == bytePattern.length) {

					matchPoint = i - bytePattern.length + 1;

					if ((text.length - i) > bytePattern.length) {

						j = 0;

						continue;

					}

					return matchPoint;

				}

				// 如果从中间某个位置找，找到末尾没找到后，再重头开始找

				if (startIndex != 0 && i + 1 == end) {

					end = startIndex;

					i = -1;

					startIndex = 0;

				}

			}

			return matchPoint;

		}

		/**
		 *
		 * 找到末尾后不会重头开始找
		 *
		 *
		 *
		 * @param text
		 *            of type byte[]
		 *
		 * @param startIndex
		 *            of type int
		 *
		 * @return int
		 */

		public int lastIndexOfWithNoLoop(byte[] text, int startIndex) {

			matchPoint = -1;

			int j = 0;

			if (text.length == 0 || startIndex > text.length)
				return -1;

			for (int i = startIndex; i < text.length; i++) {

				while (j > 0 && bytePattern[j] != text[i]) {

					j = failure[j - 1];

				}

				if (bytePattern[j] == text[i]) {

					j++;

				}

				if (j == bytePattern.length) {

					matchPoint = i - bytePattern.length + 1;

					if ((text.length - i) > bytePattern.length) {

						j = 0;

						continue;

					}

					return matchPoint;

				}

			}

			return matchPoint;

		}

		/**
		 *
		 * Method computeFailure4Byte …
		 *
		 *
		 *
		 * @param patternStr
		 *            of type byte[]
		 */

		public void computeFailure4Byte(byte[] patternStr) {

			bytePattern = patternStr;

			int j = 0;

			int len = bytePattern.length;

			failure = new int[len];

			for (int i = 1; i < len; i++) {

				while (j > 0 && bytePattern[j] != bytePattern[i]) {

					j = failure[j - 1];

				}

				if (bytePattern[j] == bytePattern[i]) {

					j++;

				}

				failure[i] = j;

			}

		}

	}

//	public static short[] toShortArray(byte[] src) {
//
//		int count = src.length >> 1;
//		short[] dest = new short[count];
//		for (int i = 0; i < count; i++) {
//			dest[i] = (short) (src[i * 2] << 8 | src[2 * i + 1] & 0xff);
//		}
//		return dest;
//	}
//
//	public static byte[] toByteArray(short[] src) {
//
//		int count = src.length;
//		byte[] dest = new byte[count << 1];
//		for (int i = 0; i < count; i++) {
//			dest[i * 2] = (byte) (src[i] >> 8);
//			dest[i * 2 + 1] = (byte) (src[i] >> 0);
//		}
//
//		return dest;
//	}



}