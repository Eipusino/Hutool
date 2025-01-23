package cn.hutool.core.annotation.scanner;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;

/**
 * 默认不扫描任何元素的扫描器
 *
 * @author huangchengxing
 */
public class EmptyAnnotationScanner implements AnnotationScanner {

	@Override
	public boolean support(AnnotatedElement annotatedEle) {
		return true;
	}

	@Override
	public List<Annotation> getAnnotations(AnnotatedElement annotatedEle) {
		return Collections.emptyList();
	}

	@Override
	public void scan(BiConsumer<Integer, Annotation> consumer, AnnotatedElement annotatedEle, Predicate<Annotation> filter) {
		// do nothing
	}
}
