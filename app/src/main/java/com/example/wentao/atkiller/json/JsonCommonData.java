package com.example.wentao.atkiller.json;

public class JsonCommonData {
    public void setAllData(String op, String dataType, String id, String name, long dataLength) {
        this.op = op;
        this.dataType = dataType;
        this.id = id;
        this.name = name;
        this.dataLength = dataLength;
    }

    private String op;
    private String dataType;
    private String id;
    private String name;
    private long dataLength;

    public String getOp() {
        return op;
    }

    public void setOp(String op) {
        this.op = op;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getDataLength() {
        return dataLength;
    }

    public void setDataLength(long dataLength) {
        this.dataLength = dataLength;
    }
}
