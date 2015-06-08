/**
 * Pinganfu.com Inc.
 * Copyright (c) 2003-2013 All Rights Reserved.
 */
package services.goldway.util;

import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.QName;
import org.dom4j.io.SAXReader;

public final class XmlHelper {

	private XmlHelper() {
	}

	public static Map<String, String> convertPrototype(String messagePrototype)
			throws DocumentException {
		List<Element> fields = getFields(messagePrototype, "map");

		Map<String, String> paramMap = new LinkedHashMap<String, String>();
		for (Element field : fields) {
			String fieldName = field.attributeValue("name");
			String fieldValue = field.getText();
			paramMap.put(fieldName, fieldValue);
		}

		return paramMap;
	}

	public static Map<String, String> getParams(Element paramsElement) {
		LinkedHashMap<String, String> params = new LinkedHashMap<String, String>();
		List<Element> paramElements = children(paramsElement, "param");
		for (Element param : paramElements) {

			// 鐟曚椒绠炵亸鍙樼瑝鐟曚線鍘ょ純顕嗙礉鐟曚椒绠炵亸杈渽閹稿洤鐣鹃柊宥囩枂閸愬懎顔�?			
			String text = param.getText();
			params.put(param.attributeValue("name"), text);
		}

		return params;
	}

	public static List<Element> getFields(String xml, String node)
			throws DocumentException {
		Element root = getField(xml);
		return children(root, node);
	}

	public static List<Element> getFields(String xml, String node,
			String subNode) throws DocumentException {
		Element root = getField(xml);
		Element nodeElement = child(root, node);
		return children(nodeElement, subNode);
	}

	public static Element getField(String xml) throws DocumentException {
		StringReader stringReader = null;

		try {
			stringReader = new StringReader(xml);
			SAXReader reader = new SAXReader();

			Document doc = reader.read(stringReader);
			return doc.getRootElement();
		} finally {
			IOUtils.closeQuietly(stringReader);
		}

	}

	/**
	 * Return the child element with the given name. The element must be in the
	 * same name space as the parent element.
	 * 
	 * @param element
	 *            The parent element
	 * @param name
	 *            The child element name
	 * @return The child element
	 */
	public static Element child(Element element, String name) {
		return element.element(new QName(name, element.getNamespace()));
	}

	/**
	 * Return the descendant element with the given xPath. Remember to remove
	 * the heading and tailing backslash ( / )
	 * 
	 * @param element
	 *            The parent element
	 * @param xPath
	 *            e.g: "foo/bar"
	 * @return The child element. Return null if any sub node name is not
	 *         matched
	 */
	public static Element descendant(Element element, String xPath) {
		if (element == null || xPath == null || xPath.trim().isEmpty()) {
			return null;
		}

		String[] paths = xPath.split("/");

		Element tempElement = element;
		for (String nodeName : paths) {
			tempElement = child(tempElement, nodeName);
		}
		return tempElement;
	}

	/**
	 * Return the child elements with the given name. The elements must be in
	 * the same name space as the parent element.
	 * 
	 * @param element
	 *            The parent element
	 * @param name
	 *            The child element name
	 * @return The child elements
	 */
	@SuppressWarnings("unchecked")
	public static List<Element> children(Element element, String name) {
		return element.elements(new QName(name, element.getNamespace()));
	}

	/**
	 * Return the value of the child element with the given name. The element
	 * must be in the same name space as the parent element.
	 * 
	 * @param element
	 *            The parent element
	 * @param name
	 *            The child element name
	 * @return The child element value
	 */
	public static String elementAsString(Element element, String name) {
		return element.elementTextTrim(new QName(name, element.getNamespace()));
	}

	/**
	 */
	public static int elementAsInteger(Element element, String name) {
		String text = elementAsString(element, name);
		if (text == null) {
			return 0;
		}

		return Integer.parseInt(text);
	}

	
	public static boolean elementAsBoolean(Element element, String name) {
		String text = elementAsString(element, name);
		if (text == null) {
			return false;
		}
		return Boolean.valueOf(text);
	}

}
