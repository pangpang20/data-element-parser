package com.datagov.model;

import java.util.List;

public class RuleRequest {
    private List<DataElement> dataElements;

    public List<DataElement> getDataElements() { return dataElements; }
    public void setDataElements(List<DataElement> dataElements) { this.dataElements = dataElements; }
}
