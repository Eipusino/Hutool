package cn.hutool.core.convert.impl;

import cn.hutool.core.convert.*;
import cn.hutool.core.util.*;

import java.nio.charset.*;

/**
 * 编码对象转换器
 * @author Looly
 *
 */
public class CharsetConverter extends AbstractConverter<Charset>{
	private static final long serialVersionUID = 1L;

	@Override
	protected Charset convertInternal(Object value) {
		return CharsetUtil.charset(convertToStr(value));
	}

}
