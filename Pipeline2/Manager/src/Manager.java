import com.java_polytech.config_support.SyntaxAnalyzer;
import com.java_polytech.pipeline_interfaces.*;
import javafx.util.Pair;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import static com.java_polytech.pipeline_interfaces.RC.RC_MANAGER_INVALID_ARGUMENT;
import static com.java_polytech.pipeline_interfaces.RC.RC_SUCCESS;

import java.util.ArrayList;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Manager implements IConfigurable {
    IReader ReaderElement;
    ArrayList<IExecutor> ExecutorElements;
    IWriter WriterElement;
    boolean IsInited;
    FileInputStream FileIn;
    FileOutputStream FileOut;
    // custom return codes
    public static final RC RC_MANAGER_NOT_INITED = new RC(RC.RCWho.MANAGER, RC.RCType.CODE_CUSTOM_ERROR, "Class not inited");
    private static Logger logger;

    public static boolean handleRC(RC returnCode)
    {
        if (!returnCode.isSuccess())
        {
            logger.severe("ERROR: " + returnCode.who.get() + ": " + returnCode.info);
            return false;
        }
        return true;
    }

    public Manager()
    {
        IsInited = false;
        ExecutorElements = new ArrayList<IExecutor>();
    }

    public RC setConfig(String var1)
    {
        SyntaxAnalyzer config = new SyntaxAnalyzer(RC.RCWho.MANAGER, new ManagerConfig());
        RC code = config.process(var1);
        if (code.isSuccess())
        {
            // open files
            try {
                Pair<RC, ArrayList<String>> val = config.GetField(ManagerConfig.ConfigFields.IN_FILENAME.asString());
                if (!val.getKey().isSuccess())
                    return val.getKey();
                if (val.getValue().size() != 1)
                    return new RC(RC.RCWho.MANAGER, RC.RCType.CODE_CUSTOM_ERROR, "IN_FILENAME must be 1 value, not an array");

                FileIn = new FileInputStream(val.getValue().get(0));
            } catch (FileNotFoundException e) {
                return RC.RC_MANAGER_INVALID_INPUT_FILE;
            }

            try {
                Pair<RC, ArrayList<String>> val = config.GetField(ManagerConfig.ConfigFields.OUT_FILENAME.asString());
                if (!val.getKey().isSuccess())
                    return val.getKey();
                if (val.getValue().size() != 1)
                    return new RC(RC.RCWho.MANAGER, RC.RCType.CODE_CUSTOM_ERROR, "OUT_FILENAME must be 1 value, not an array");

                FileOut = new FileOutputStream(val.getValue().get(0));
            } catch (FileNotFoundException e) {
                return RC.RC_MANAGER_INVALID_OUTPUT_FILE;
            }

            // Get all classes
            try {
                Pair<RC, ArrayList<String>> val = config.GetField(ManagerConfig.ConfigFields.READER_NAME.asString());
                if (!val.getKey().isSuccess())
                    return val.getKey();
                if (val.getValue().size() != 1)
                    return new RC(RC.RCWho.MANAGER, RC.RCType.CODE_CUSTOM_ERROR, "READER_NAME must be 1 value, not an array");

                Class<?> reader = Class.forName(val.getValue().get(0));
                if (IReader.class.isAssignableFrom(reader)) {
                    ReaderElement = (IReader) reader.getDeclaredConstructor().newInstance();
                }
                else {
                    return RC.RC_MANAGER_INVALID_READER_CLASS;
                }
            }
            catch (Exception e) {
                return RC.RC_MANAGER_INVALID_READER_CLASS;
            }

            {
                Pair<RC, ArrayList<String>> val = config.GetField(ManagerConfig.ConfigFields.EXECUTORS_NAMES.asString());
                if (!val.getKey().isSuccess())
                    return val.getKey();

                for (int number = 0; number < val.getValue().size(); number++) {
                    try {
                        Class<?> executor = Class.forName(val.getValue().get(number));
                        if (IExecutor.class.isAssignableFrom(executor)) {
                            ExecutorElements.add((IExecutor) executor.getDeclaredConstructor().newInstance());
                        } else {
                            return new RC(RC.RCWho.MANAGER, RC.RCType.CODE_CUSTOM_ERROR, "Invalid Executor number #".concat(String.valueOf(number)));
                        }
                    } catch (Exception e) {
                        return new RC(RC.RCWho.MANAGER, RC.RCType.CODE_CUSTOM_ERROR, "Invalid Executor number #".concat(String.valueOf(number)));
                    }
                }
            }

            try {
                Pair<RC, ArrayList<String>> val = config.GetField(ManagerConfig.ConfigFields.WRITER_NAME.asString());
                if (!val.getKey().isSuccess())
                    return val.getKey();
                if (val.getValue().size() != 1)
                    return new RC(RC.RCWho.MANAGER, RC.RCType.CODE_CUSTOM_ERROR, "WRITER_NAME must be 1 value, not an array");

                Class<?> writer = Class.forName(val.getValue().get(0));
                if (IWriter.class.isAssignableFrom(writer)) {
                    WriterElement = (IWriter) writer.getDeclaredConstructor().newInstance();
                }
                else {
                    return RC.RC_MANAGER_INVALID_WRITER_CLASS;
                }
            }
            catch (Exception e) {
                return RC.RC_MANAGER_INVALID_WRITER_CLASS;
            }

            // init all them
            RC tmp;
            Pair<RC, ArrayList<String>> val = config.GetField(ManagerConfig.ConfigFields.READER_CONFIG.asString());
            if (!val.getKey().isSuccess())
                return val.getKey();
            if (val.getValue().size() != 1)
                return new RC(RC.RCWho.MANAGER, RC.RCType.CODE_CUSTOM_ERROR, "READER_CONFIG must be 1 value, not an array");

            tmp = ReaderElement.setConfig(val.getValue().get(0));
            if (!tmp.isSuccess())
                return tmp;

            {
                val = config.GetField(ManagerConfig.ConfigFields.EXECUTORS_CONFIGS.asString());
                if (!val.getKey().isSuccess())
                    return val.getKey();

                if (val.getValue().size() != ExecutorElements.size())
                    return new RC(RC.RCWho.MANAGER, RC.RCType.CODE_CUSTOM_ERROR, "Number of EXECUTORS_CONFIGS and EXECUTORS_NAMES does not match");
                for (int number = 0; number < ExecutorElements.size(); number++)
                {
                    tmp = ExecutorElements.get(number).setConfig(val.getValue().get(number));
                    if (!tmp.isSuccess())
                        return tmp;
                }
            }


            val = config.GetField(ManagerConfig.ConfigFields.WRITER_CONFIG.asString());
            if (!val.getKey().isSuccess())
                return val.getKey();
            if (val.getValue().size() != 1)
                return new RC(RC.RCWho.MANAGER, RC.RCType.CODE_CUSTOM_ERROR, "WRITER_CONFIG must be 1 value, not an array");


            tmp = WriterElement.setConfig(val.getValue().get(0));
            if (!tmp.isSuccess())
                return tmp;

            // place them with all other initing

            tmp = ReaderElement.setInputStream(FileIn);
            if (!tmp.isSuccess())
                return tmp;

            tmp = ReaderElement.setConsumer(ExecutorElements.get(0));
            if (!tmp.isSuccess())
                return tmp;

            for (int i = 0; i < ExecutorElements.size() - 1; i++)
            {
                tmp = ExecutorElements.get(i).setConsumer(ExecutorElements.get(i + 1));
                if (!tmp.isSuccess())
                    return tmp;
            }

            tmp = ExecutorElements.get(ExecutorElements.size() - 1).setConsumer(WriterElement);
            if (!tmp.isSuccess())
                return tmp;

            tmp = WriterElement.setOutputStream(FileOut);
            if (!tmp.isSuccess())
                return tmp;

            IsInited = true;
            return RC_SUCCESS;
        }
        return code;
    }

    public RC execute()
    {
        if (!IsInited)
            return RC_MANAGER_NOT_INITED;

        RC tmp_rc = ReaderElement.run();
        try {
            FileIn.close();
            FileOut.close();
        }
        catch (IOException e) {
            handleRC(new RC(RC.RCWho.MANAGER, RC.RCType.CODE_CUSTOM_ERROR, "ERROR: Manager: cannot even close files"));
        }
        return tmp_rc;
    }

    static final String logFileName = "log.txt";

    private static Logger makeLogger() {
        Logger logger = Logger.getLogger("Logger");
        FileHandler fh;
        try {
            fh = new FileHandler(logFileName);
        } catch (IOException ex) {
            return null;
        }
        SimpleFormatter sf = new SimpleFormatter();
        fh.setFormatter(sf);
        logger.addHandler(fh);
        logger.setUseParentHandlers(false);

        return logger;
    }

    public static void main(String[] args) {
        logger = makeLogger();

        if (args.length != 1) {
            handleRC(RC_MANAGER_INVALID_ARGUMENT);
            return;
        }

        Manager manager = new Manager();
        if (handleRC(manager.setConfig(args[0]))) {
            if (handleRC(manager.execute())) {
                System.out.println("Everything succeed!");
                return;
            }
        }
        System.out.println("Something goes wrong! Check log.txt for more info!");
    }
}
