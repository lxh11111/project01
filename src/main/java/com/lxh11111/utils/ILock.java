package com.lxh11111.utils;

public interface ILock {
    //尝试获取锁
    boolean trylock(long timeoutSec);
    //释放锁
    void unlock();
}
