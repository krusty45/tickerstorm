package io.tickerstorm.data.converter;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.io.Files;

import io.tickerstorm.common.data.converter.BaseFileConverter;
import io.tickerstorm.common.entity.Candle;
import io.tickerstorm.common.entity.MarketData;

@Component
public class StooqFileConverter extends BaseFileConverter {

  private static final java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  private static final java.time.format.DateTimeFormatter dayFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd");

  private static final Logger logger = LoggerFactory.getLogger(StooqFileConverter.class);

  private Set<String> securityTypes = new HashSet<String>();

  @PostConstruct
  private void init() {
    securityTypes.add("currencies");
    securityTypes.add("indices");
  }

  @Override
  public MarketData[] convert(String path) {

    File f = new File(path);
    List<MarketData> data = new ArrayList<MarketData>();

    if (f.isFile()) {

      String symbol = f.getName().substring(0, f.getName().indexOf("."));

      for (String s : securityTypes) {

        if (f.getPath().contains(s)) {

          List<String> lines = new ArrayList<String>();

          try {
            lines = IOUtils.readLines(new FileInputStream(new File(path)));
          } catch (Exception e) {
            logger.error("Unable to read lines from file " + path);
            continue;
          }

          for (String line : lines) {

            if (line.startsWith("Date"))
              continue;

            Candle c = null;
            try {

              String[] cols = line.split(",");

              try {

                c = new Candle(symbol, provider(), LocalDateTime.parse(cols[0] + " " + cols[1], formatter).toInstant(ZoneOffset.UTC),
                    new BigDecimal(cols[2]), new BigDecimal(cols[5]), new BigDecimal(cols[3]), new BigDecimal(cols[4]), interval(path),
                    new Integer(cols[6]));

                // c.timestamp = LocalDateTime.parse(cols[0] + " " + cols[1], formatter)
                // .toInstant(ZoneOffset.UTC);
                // c.open = new BigDecimal(cols[2]);
                // c.high = new BigDecimal(cols[3]);
                // c.low = new BigDecimal(cols[4]);
                // c.close = new BigDecimal(cols[5]);
                // c.volume = new Integer(cols[6]);

              } catch (Exception ex) {

                c = new Candle(symbol, provider(), LocalDateTime.from(dayFormatter.parse(cols[0])).toInstant(ZoneOffset.of("GMT")),
                    new BigDecimal(cols[1]), new BigDecimal(cols[4]), new BigDecimal(cols[2]), new BigDecimal(cols[3]), interval(path),
                    new Integer(cols[5]));

                // c.timestamp =
                // LocalDateTime.from(dayFormatter.parse(cols[0])).toInstant(ZoneOffset.of("GMT"));
                // c.open = new BigDecimal(cols[1]);
                // c.high = new BigDecimal(cols[2]);
                // c.low = new BigDecimal(cols[3]);
                // c.close = new BigDecimal(cols[4]);
                // c.volume = new Integer(cols[5]);
              }

              if (BigInteger.ZERO.equals(c.volume)) {
                c.setVolume(null);
              }

              // c.interval = interval(path);
              // c.source = provider();

            } catch (Exception e) {
              logger.error("Unable to parse symbol " + symbol, e.getMessage());
              continue;
            }

            historical.publishAsync(c);
            data.add(c);
          }
        }
      }
    }

    return data.toArray(new MarketData[] {});

  }

  private String interval(String path) {

    if (path.contains("5 min"))
      return Candle.MIN_5_INTERVAL;

    if (path.contains("hourly"))
      return Candle.HOURLY_INTERVAL;

    if (path.contains("daily"))
      return Candle.EOD;

    return Candle.MIN_5_INTERVAL;

  }

  @Override
  public String provider() {
    return "Stooq";
  }

  @Override
  public void onFileCreate(File file) {

    File f = new File(file.getPath().replace("\\", "\\\\"));

    if (file.getPath().contains(provider()) && Files.getFileExtension(file.getPath()).equals("txt")) {
      logger.info("Converting " + file.getName());
      long start = System.currentTimeMillis();
      MarketData[] data = convert(f.getPath());
      logger.info("Converted " + data.length + " records.");
      logger.debug("Conversion took " + (System.currentTimeMillis() - start) + "ms");
      FileUtils.deleteQuietly(file);
    }
  }

  @Override
  public void onDirectoryChange(File file) {

    if (file.getPath().contains(provider()) && !file.getName().equalsIgnoreCase(provider())) {
      if (file.isDirectory() && file.list().length == 0) {
        logger.info("Deleting " + file.getPath() + " since it's empty");
        FileUtils.deleteQuietly(file);
      }
    }
  }
}
