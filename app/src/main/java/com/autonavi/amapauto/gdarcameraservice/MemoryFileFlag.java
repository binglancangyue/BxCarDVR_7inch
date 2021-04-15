package com.autonavi.amapauto.gdarcameraservice;

/**
 * 共享内存文件状态标志类
 */
public enum MemoryFileFlag {
    /**
     * 可读标志
     */
    CAN_READ((byte)0),
    /**
     * 可写标志
     */
    CAN_WRITE((byte)1);

    private byte flag;

    private MemoryFileFlag(byte flag) {
        this.flag = flag;
    }

    public byte getFlag() {
        return this.flag;
    }
}