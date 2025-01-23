package cn.hutool.core.bean.copier;

import cn.hutool.core.bean.*;
import cn.hutool.core.lang.*;
import cn.hutool.core.util.*;

import java.lang.reflect.*;
import java.util.*;

/**
 * {@link ValueProvider}属性拷贝到Bean中的拷贝器
 *
 * @param <T> 目标Bean类型
 * @since 5.8.0
 */
public class ValueProviderToBeanCopier<T> extends AbsCopier<ValueProvider<String>, T> {

	/**
	 * 目标的类型（用于泛型类注入）
	 */
	private final Type targetType;

	/**
	 * 构造
	 *
	 * @param source      来源Map
	 * @param target      目标Bean对象
	 * @param targetType  目标泛型类型
	 * @param copyOptions 拷贝选项
	 */
	public ValueProviderToBeanCopier(ValueProvider<String> source, T target, Type targetType, CopyOptions copyOptions) {
		super(source, target, copyOptions);
		this.targetType = targetType;
	}

	@Override
	public T copy() {
		Class<?> actualEditable = target.getClass();
		if (null != copyOptions.editable) {
			// 检查限制类是否为target的父类或接口
			Assert.isTrue(copyOptions.editable.isInstance(target),
					"Target class [{}] not assignable to Editable class [{}]", actualEditable.getName(), copyOptions.editable.getName());
			actualEditable = copyOptions.editable;
		}
		final Map<String, PropDesc> targetPropDescMap = BeanUtil.getBeanDesc(actualEditable).getPropMap(copyOptions.ignoreCase);

		targetPropDescMap.forEach((tFieldName, tDesc) -> {
			if (null == tFieldName) {
				return;
			}
			tFieldName = copyOptions.editFieldName(tFieldName);
			// 对key做转换，转换后为null的跳过
			if (null == tFieldName) {
				return;
			}

			// 无字段内容跳过
			if(!source.containsKey(tFieldName)){
				return;
			}

			// 忽略不需要拷贝的 key,
			if (!copyOptions.testKeyFilter(tFieldName)) {
				return;
			}

			// 检查目标字段可写性
			if (null == tDesc || !tDesc.isWritable(this.copyOptions.transientSupport)) {
				// 字段不可写，跳过之
				return;
			}

			// 获取目标字段真实类型
			final Type fieldType = TypeUtil.getActualType(this.targetType ,tDesc.getFieldType());

			// 检查目标对象属性是否过滤属性
			Object sValue = source.value(tFieldName, fieldType);
			if (!copyOptions.testPropertyFilter(tDesc.getField(), sValue)) {
				return;
			}

			// 自定义值
			sValue = copyOptions.editFieldValue(tFieldName, sValue);

			// 目标赋值
			tDesc.setValue(this.target, sValue, copyOptions.ignoreNullValue, copyOptions.ignoreError, copyOptions.override);
		});
		return this.target;
	}
}
