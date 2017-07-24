package cn.ryan.utils;

import java.io.IOException;
import java.io.StringReader;

import org.wltea.analyzer.core.IKSegmenter;
import org.wltea.analyzer.core.Lexeme;

/***
 * 中文分词器
 * 
 * @author HuRui
 *
 */
public class ChineseSpliter {
	/**
	 * 对给定的文本进行中文分词
	 * 
	 * @param text
	 *            给定的文本
	 * @param splitToken
	 *            用于分割的标记,如"|"
	 * @return 分词完毕的文本
	 */
	public static String split(String text, String splitToken) {
		String result = "";
		try {
			StringReader reader = new StringReader(removeCharacter(text));
			IKSegmenter ik = new IKSegmenter(reader, true);// 当为true时，分词器进行最大词长切分
			Lexeme lexeme = null;
			while ((lexeme = ik.next()) != null) {
				// System.out.print(lexeme.getLexemeText()+splitToken);
				result += lexeme.getLexemeText() + splitToken;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}

	/***
	 * 对给定的文本进行中文分词，以" "(空格)分割
	 * 
	 * @param text
	 *            给定的文本
	 * @return
	 */
	public static String split(String text) {
		return split(text, " ");
	}

	public static String[] splitArr(String text) {
		if (StringUtils.isNullOrEmpty(text)) {
			return null;
		}
		return split(text).split(" ");
	}

	private static String removeCharacter(String text) {
		return text.replaceAll("\\s*|\t|\r|\n", "");
	}

}