package cn.hutool.core.convert.impl;

import cn.hutool.core.convert.*;

import java.util.*;

/**
 * TimeZone转换器
 * @author Looly
 *
 */
public class TimeZoneConverter extends AbstractConverter<TimeZone>{
	private static final long serialVersionUID = 1L;

	@Override
	protected TimeZone convertInternal(Object value) {
		return TimeZone.getTimeZone(convertToStr(value));
	}

}
