package cn.hutool.core.convert.impl;

import cn.hutool.core.bean.*;
import cn.hutool.core.convert.*;
import cn.hutool.core.lang.*;
import cn.hutool.core.map.*;
import cn.hutool.core.util.*;

import java.lang.reflect.*;
import java.util.*;

public class EntryConverter extends AbstractConverter<Map.Entry<?, ?>> {

	/** Pair类型 */
	private final Type pairType;
	/** 键类型 */
	private final Type keyType;
	/** 值类型 */
	private final Type valueType;

	/**
	 * 构造，Pair的key和value泛型类型自动获取
	 *
	 * @param entryType Map类型
	 */
	public EntryConverter(Type entryType) {
		this(entryType, TypeUtil.getTypeArgument(entryType, 0), TypeUtil.getTypeArgument(entryType, 1));
	}

	/**
	 * 构造
	 *
	 * @param entryType Pair类型
	 * @param keyType 键类型
	 * @param valueType 值类型
	 */
	public EntryConverter(Type entryType, Type keyType, Type valueType) {
		this.pairType = entryType;
		this.keyType = keyType;
		this.valueType = valueType;
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected Map.Entry<?, ?> convertInternal(Object value) {
		Map map = null;
		if (value instanceof Pair) {
			final Pair pair = (Pair) value;
			map = MapUtil.of(pair.getKey(), pair.getValue());
		}else if (value instanceof Map) {
			map = (Map) value;
		} else if (value instanceof CharSequence) {
			final CharSequence str = (CharSequence) value;
			map = strToMap(str);
		} else if (BeanUtil.isReadableBean(value.getClass())) {
			map = BeanUtil.beanToMap(value);
		}

		if (null != map) {
			return mapToEntry(pairType, keyType, valueType, map);
		}

		throw new ConvertException("Unsupported to map from [{}] of type: {}", value, value.getClass().getName());
	}

	/**
	 * 字符串转单个键值对的Map，支持分隔符{@code :}、{@code =}、{@code ,}
	 *
	 * @param str 字符串
	 * @return map or null
	 */
	private static Map<CharSequence, CharSequence> strToMap(final CharSequence str) {
		// key:value  key=value  key,value
		final int index = StrUtil.indexOf(str, '=', 0, str.length());

		if (index > -1) {
			return MapUtil.of(str.subSequence(0, index + 1), str.subSequence(index, str.length()));
		}
		return null;
	}

	/**
	 * Map转Entry
	 *
	 * @param targetType 目标的Map类型
	 * @param keyType    键类型
	 * @param valueType  值类型
	 * @param map        被转换的map
	 * @return Entry
	 */
	@SuppressWarnings("rawtypes")
	private static Map.Entry<?, ?> mapToEntry(final Type targetType, final Type keyType, final Type valueType, final Map map) {

		Object key = null;
		Object value = null;
		if (1 == map.size()) {
			final Map.Entry entry = (Map.Entry) map.entrySet().iterator().next();
			key = entry.getKey();
			value = entry.getValue();
		} else if (2 == map.size()) {
			key = map.get("key");
			value = map.get("value");
		}

		final ConverterRegistry convert = ConverterRegistry.getInstance();
		return (Map.Entry<?, ?>) ReflectUtil.newInstance(TypeUtil.getClass(targetType),
			TypeUtil.isUnknown(keyType) ? key : convert.convert(keyType, key),
			TypeUtil.isUnknown(valueType) ? value : convert.convert(valueType, value)
		);
	}
}
