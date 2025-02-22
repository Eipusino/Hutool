package cn.hutool.core.thread.lock;

import java.util.concurrent.*;
import java.util.concurrent.locks.*;

/**
 * 无锁实现
 *
 * @author looly
 *@since 4.3.1
 */
public class NoLock implements Lock{

	public static NoLock INSTANCE = new NoLock();

	@Override
	public void lock() {
	}

	@Override
	public void lockInterruptibly() {
	}

	@Override
	public boolean tryLock() {
		return true;
	}

	@SuppressWarnings("NullableProblems")
	@Override
	public boolean tryLock(long time, TimeUnit unit) {
		return true;
	}

	@Override
	public void unlock() {
	}

	@SuppressWarnings("NullableProblems")
	@Override
	public Condition newCondition() {
		throw new UnsupportedOperationException("NoLock`s newCondition method is unsupported");
	}

}
