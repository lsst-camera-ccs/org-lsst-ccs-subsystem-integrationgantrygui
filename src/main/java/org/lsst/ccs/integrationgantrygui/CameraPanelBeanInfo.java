package org.lsst.ccs.integrationgantrygui;

import java.beans.BeanDescriptor;
import java.beans.SimpleBeanInfo;


public class CameraPanelBeanInfo extends SimpleBeanInfo {

    @Override
    public BeanDescriptor getBeanDescriptor() {
        BeanDescriptor desc = new BeanDescriptor(CameraPanelBeanInfo.class);
        desc.setValue("isContainer", Boolean.FALSE);
        return desc;
    }
}
