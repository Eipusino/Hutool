package cn.hutool.core.bean;

import cn.hutool.core.lang.func.*;
import cn.hutool.core.map.*;

/**
 * Bean属性缓存<br>
 * 缓存用于防止多次反射造成的性能问题
 *
 * @author Looly
 */
public enum BeanDescCache {
	INSTANCE;

	private final WeakConcurrentMap<Class<?>, BeanDesc> bdCache = new WeakConcurrentMap<>();

	/**
	 * 获得属性名和{@link BeanDesc}Map映射
	 *
	 * @param beanClass Bean的类
	 * @param supplier  对象不存在时创建对象的函数
	 * @return 属性名和{@link BeanDesc}映射
	 * @since 5.4.2
	 */
	public BeanDesc getBeanDesc(Class<?> beanClass, Func0<BeanDesc> supplier) {
		return bdCache.computeIfAbsent(beanClass, (key)->supplier.callWithRuntimeException());
	}

	/**
	 * 清空全局的Bean属性缓存
	 *
	 * @since 5.7.21
	 */
	public void clear() {
		this.bdCache.clear();
	}
}
