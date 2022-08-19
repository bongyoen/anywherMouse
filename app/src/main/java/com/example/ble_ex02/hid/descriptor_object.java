package com.example.ble_ex02.hid;

public class descriptor_object {
    public static final byte[] descriptor = new byte[]{
            0x05, 0x01,           // USAGE_PAGE (Generic Desktop)<font></font>
            0x09, 0x02,           // USAGE (Mouse)<font></font>
            (byte) 0xa1, 0x01,     // COLLECTION (Application)<font></font>
            0x09, 0x01,           //   USAGE (Pointer)<font></font>
            (byte) 0xa1, 0x00,     //   COLLECTION (Physical)<font></font>
            0x05, 0x09,           //     USAGE_PAGE (Button)<font></font>
            0x19, 0x01,           //     USAGE_MINIMUM (Button 1)<font></font>
            0x29, 0x03,           //     USAGE_MAXIMUM (Button 3)<font></font>
            0x15, 0x00,           //     LOGICAL_MINIMUM (0)<font></font>
            0x25, 0x01,           //     LOGICAL_MAXIMUM (1)<font></font>
            0x75, 0x01,           //     REPORT_SIZE (1)<font></font>
            (byte) 0x95, 0x03,     //     REPORT_COUNT (3)<font></font>
            (byte) 0x81, 0x02,     //     INPUT (Data,Var,Abs)<font></font>
            0x75, 0x05,           //     REPORT_SIZE (5)<font></font>
            (byte) 0x95, 0x01,     //     REPORT_COUNT (1)<font></font>
            (byte) 0x81, 0x03,     //     INPUT (Cnst,Var,Abs)<font></font>
            0x05, 0x01,           //     USAGE_PAGE (Generic Desktop)<font></font>
            0x09, 0x30,           //     USAGE (X)<font></font>
            0x09, 0x31,           //     USAGE (Y)<font></font>
            0x15, (byte) 0x81,     //     LOGICAL_MINIMUM (-127)<font></font>
            0x25, 0x7f,           //     LOGICAL_MAXIMUM (127)<font></font>
            0x75, 0x08,           //     REPORT_SIZE (8)<font></font>
            (byte) 0x95, 0x02,     //     REPORT_COUNT (2)<font></font>
            (byte) 0x81, 0x06,     //     INPUT (Data,Var,Rel)<font></font>
            (byte) 0xc0,           //   END_COLLECTION<font></font>
            (byte) 0xc0            // END_COLLECTION<font></font>
    };
}
