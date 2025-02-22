package cn.hutool.core.convert.impl;

import cn.hutool.core.bean.*;
import cn.hutool.core.convert.*;
import cn.hutool.core.map.*;
import cn.hutool.core.util.*;

import java.lang.reflect.*;
import java.util.*;

/**
 * {@link Map} 转换器
 *
 * @author Looly
 * @since 3.0.8
 */
public class MapConverter extends AbstractConverter<Map<?, ?>> {
	private static final long serialVersionUID = 1L;

	/** Map类型 */
	private final Type mapType;
	/** 键类型 */
	private final Type keyType;
	/** 值类型 */
	private final Type valueType;

	/**
	 * 构造，Map的key和value泛型类型自动获取
	 *
	 * @param mapType Map类型
	 */
	public MapConverter(Type mapType) {
		this(mapType, TypeUtil.getTypeArgument(mapType, 0), TypeUtil.getTypeArgument(mapType, 1));
	}

	/**
	 * 构造
	 *
	 * @param mapType Map类型
	 * @param keyType 键类型
	 * @param valueType 值类型
	 */
	public MapConverter(Type mapType, Type keyType, Type valueType) {
		this.mapType = mapType;
		this.keyType = keyType;
		this.valueType = valueType;
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected Map<?, ?> convertInternal(Object value) {
		Map map;
		if (value instanceof Map) {
			final Class<?> valueClass = value.getClass();
			if(valueClass.equals(this.mapType)){
				final Type[] typeArguments = TypeUtil.getTypeArguments(valueClass);
				if (null != typeArguments //
						&& 2 == typeArguments.length//
						&& Objects.equals(this.keyType, typeArguments[0]) //
						&& Objects.equals(this.valueType, typeArguments[1])) {
					//对于键值对类型一致的Map对象，不再做转换，直接返回原对象
					return (Map) value;
				}
			}

			final Class<?> mapClass = TypeUtil.getClass(this.mapType);
			if (null == mapClass || mapClass.isAssignableFrom(AbstractMap.class)) {
				// issue#I6YN2A，默认有序
				map =  new LinkedHashMap<>();
			} else{
				map = MapUtil.createMap(mapClass);
			}
			convertMapToMap((Map) value, map);
		} else if (BeanUtil.isBean(value.getClass())) {
			if(value.getClass().getName().equals("cn.hutool.json.JSONArray")){
				// issue#3795 增加JSONArray转Map错误检查
				throw new UnsupportedOperationException(StrUtil.format("Unsupported {} to Map.", value.getClass().getName()));
			}

			map = BeanUtil.beanToMap(value);
			// 二次转换，转换键值类型
			map = convertInternal(map);
		} else {
			throw new UnsupportedOperationException(StrUtil.format("Unsupported toMap value type: {}", value.getClass().getName()));
		}
		return map;
	}

	/**
	 * Map转Map
	 *
	 * @param srcMap 源Map
	 * @param targetMap 目标Map
	 */
	private void convertMapToMap(Map<?, ?> srcMap, Map<Object, Object> targetMap) {
		final ConverterRegistry convert = ConverterRegistry.getInstance();
		srcMap.forEach((key, value)->{
			key = TypeUtil.isUnknown(this.keyType) ? key : convert.convert(this.keyType, key);
			value = TypeUtil.isUnknown(this.valueType) ? value : convert.convert(this.valueType, value);
			targetMap.put(key, value);
		});
	}

	@Override
	@SuppressWarnings("unchecked")
	public Class<Map<?, ?>> getTargetType() {
		return (Class<Map<?, ?>>) TypeUtil.getClass(this.mapType);
	}
}
