package com.shoonya.trade_server.service;

import com.shoonya.trade_server.config.IntradayConfig;
import com.shoonya.trade_server.config.ShoonyaConfig;
import com.shoonya.trade_server.entity.NfoSymbols;
import com.shoonya.trade_server.entity.NseSymbols;
import com.shoonya.trade_server.repositories.NfoSymbolsRepository;
import com.shoonya.trade_server.repositories.NseSymbolsRepository;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

import java.io.*;
import java.net.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Getter
@Setter
@Service
public class StartupService {

    //    @Autowired
    ShoonyaConfig shoonyaConfig;

    private Map<String, Table> dataFrames;
    private List<ShoonyaConfig.Exchange> exchanges;
    private List<IntradayConfig.Index> indexes;
    private NfoSymbolsRepository nfoSymbolsRepository;
    private NseSymbolsRepository nseSymbolsRepository;

    public StartupService(ShoonyaConfig shoonyaConfig, IntradayConfig intradayConfig,
                          NfoSymbolsRepository nfoSymbolsRepository, NseSymbolsRepository nseSymbolsRepository) {

        this.exchanges = shoonyaConfig.getExchanges();
        this.indexes = intradayConfig.getIndexes();
        this.dataFrames = new HashMap();
        this.nfoSymbolsRepository = nfoSymbolsRepository;
        this.nseSymbolsRepository = nseSymbolsRepository;
    }

    private static final Logger logger = LoggerFactory.getLogger(StartupService.class.getName());

    // TODO: improve error handling, dont throw it in function, but catch it, since function cant catch more than one exception
    // TODO: learn exactly how error handling works
    
    @PostConstruct
    public void init() throws IOException, URISyntaxException {
        logger.info("Running code at startup using @PostConstruct!");
        logger.info("Downloading the daily token files");

        String saveDir = "/tmp/shoonya";
        downloadTokenFiles(saveDir);
    }

    public void downloadTokenFiles(String saveDir) throws IOException, URISyntaxException {
        for(ShoonyaConfig.Exchange exch:exchanges){

            logger.info("name {}, uri {}", exch.getName(),exch.getFileUri());
            URI uri = new URI(exch.getFileUri());
            URL url = uri.toURL();
            String saveFile = saveDir + '/' + exch.getName() + ".zip";

            try  {

                // Extract the directory and ensure it exists
                File file = new File(saveFile);
                File parentDir = file.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    if (parentDir.mkdirs()) {
                        logger.info("Created missing directories: {}" , parentDir.getAbsolutePath());
                    } else {
                        throw new IOException("Failed to create directories: " + parentDir.getAbsolutePath());
                    }
                }


                URLConnection connection = url.openConnection();
            // Get the input stream of the connection (the file content)

                // Get the input stream of the connection (the file content)
                InputStream inputStream = connection.getInputStream();
                // Create an output stream to save the file
                FileOutputStream outputStream = new FileOutputStream(saveFile);

                byte[] buffer = new byte[4096];
                int bytesRead;

                // Read the file content and write it to the local file
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                // Close the output stream
                outputStream.close();
                logger.info("File downloaded to: {} ", saveFile);
                extractFile(saveFile, saveDir, exch.getName());
            } catch (IOException e) {
                logger.error("Error downloading file: {}", e.getMessage());
            }

        }
    }

    public void extractFile(String zipFilePath, String destDir, String exch) throws IOException {
        File destDirectory = new File(destDir);
        if (!destDirectory.exists()) {
            if (destDirectory.mkdirs()) {
                logger.info("Created missing directories: {}", destDir);
            } else {
                throw new IOException("Failed to create destination directory: " + destDir);
            }
        }

        // Open the ZIP file
        try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFilePath))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                logger.info("Extracting: {}" , entry.getName());

                // Construct the full path for the extracted file or directory
                File extractedFile = new File(destDir, entry.getName());

                if (entry.isDirectory()) {
                    // Create directories for the entry if it's a directory
                    if (!extractedFile.exists() && !extractedFile.mkdirs()) {
                        throw new IOException("Failed to create directory: " + extractedFile.getAbsolutePath());
                    }
                } else {
                    // Ensure parent directories exist
                    File parentDir = extractedFile.getParentFile();
                    if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
                        throw new IOException("Failed to create directory: " + parentDir.getAbsolutePath());
                    }

                    // Write the file content
                    try (FileOutputStream outputStream = new FileOutputStream(extractedFile)) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = zipInputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                        }
                    }
                }
                zipInputStream.closeEntry();
                logger.info("Extracted symbol file {}", entry.getName());

                //TODO: reenable them
//                loadSymbols(destDir + '/' +  entry.getName(), exch);
//                loadSymbolsToDb(destDir + '/' +  entry.getName(), exch);

            }
        } catch (IOException e) {
            logger.error("Error extracting ZIP file: {}" , e.getMessage());
        }
    }

    public void loadSymbols(String file, String exch)  {
        logger.info("loading symbol file {} from exchange {}", file, exch);
        Table table = Table.read().csv(file);
        StringColumn symbolColumn = table.stringColumn("Symbol");
        table = table.where(symbolColumn.containsString("NIFTY"));
        this.dataFrames.put(exch, table);
        logger.info("dataframe created");
    }

    public void loadSymbolsToDb(String file, String exch) {
        // #TODO: catch data to memory for faster speed -- use Redis ?
        //#TODO: shift df logic to db, uplod entire file to db instead -- too slow without redis
        //TODO: take backup of db regularly
        logger.info("loading symbol file {} from exchange {}", file, exch);
        Table table = Table.read().csv(file);

        if(exch.equals("NFO")) {
            StringColumn symbolColumn = table.stringColumn("Symbol");
            table = table.where(symbolColumn.containsString("NIFTY"));
            table.stream().forEach(row -> {
                NfoSymbols nfoSymbol = new NfoSymbols();
                nfoSymbol.setToken(row.getInt("Token"));
                nfoSymbol.setLotSize(row.getInt("LotSize"));
                nfoSymbol.setSymbol(row.getString("Symbol"));
                nfoSymbol.setTradingSymbol(row.getString("TradingSymbol"));
                nfoSymbol.setInstrument(row.getString("Instrument"));
                nfoSymbol.setOptionType(row.getString("OptionType"));
                nfoSymbol.setStrikePrice(row.getDouble("StrikePrice"));
                nfoSymbol.setTickSize(row.getDouble("TickSize"));

                StringBuilder expiry = new StringBuilder(row.getString("Expiry")); // TODO: convert column to date
                Date dt = new Date();
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH);;
                try {
                    dt = simpleDateFormat.parse(expiry.toString());
                    nfoSymbol.setExpiry(dt);
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
                nfoSymbolsRepository.save(nfoSymbol);
            });
        }
        if(exch.equals("NSE")) {
            table.stream().forEach(row -> {
                NseSymbols nseSymbol = new NseSymbols();
                nseSymbol.setToken(row.getInt("Token"));
                nseSymbol.setLotSize(row.getInt("LotSize"));
                nseSymbol.setSymbol(row.getString("Symbol"));
                nseSymbol.setTradingSymbol(row.getString("TradingSymbol"));
                nseSymbol.setInstrument(row.getString("Instrument"));
                nseSymbol.setTickSize(row.getDouble("TickSize"));
                nseSymbolsRepository.save(nseSymbol);
            });
        }
        logger.info("exchange {} uploaded to db", exch);


    }
}
