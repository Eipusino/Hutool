package cn.hutool.core.convert.impl;

import cn.hutool.core.convert.*;

import java.util.*;

/**
 * 货币{@link Currency} 转换器
 *
 * @author Looly
 * @since 3.0.8
 */
public class CurrencyConverter extends AbstractConverter<Currency> {
	private static final long serialVersionUID = 1L;

	@Override
	protected Currency convertInternal(Object value) {
		return Currency.getInstance(convertToStr(value));
	}

}
