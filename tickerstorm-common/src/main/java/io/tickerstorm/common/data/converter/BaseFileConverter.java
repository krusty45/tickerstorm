package io.tickerstorm.common.data.converter;

import java.io.File;

import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import io.tickerstorm.common.entity.MarketData;
import net.engio.mbassy.bus.MBassador;

public abstract class BaseFileConverter implements FileAlterationListener, FileConverter {

  @Qualifier("historical")
  @Autowired
  protected MBassador<MarketData> historical;

  private final static Logger logger = LoggerFactory.getLogger(BaseFileConverter.class);

  @Override
  public void onDirectoryChange(File arg0) {

  }

  @Override
  public void onDirectoryCreate(File arg0) {

  }

  @Override
  public void onDirectoryDelete(File arg0) {

  }

  @Override
  public void onFileChange(File arg0) {

  }

  @Override
  public void onFileCreate(File arg0) {

  }

  @Override
  public void onFileDelete(File arg0) {

  }

  @Override
  public void onStart(FileAlterationObserver arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onStop(FileAlterationObserver arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public Mode mode() {
    return Mode.file;
  }

}