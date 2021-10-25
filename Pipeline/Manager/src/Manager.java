import com.java_polytech.pipeline_interfaces.*;

import java.io.*;

import static com.java_polytech.pipeline_interfaces.RC.RC_MANAGER_INVALID_ARGUMENT;
import static com.java_polytech.pipeline_interfaces.RC.RC_SUCCESS;

public class Manager implements IConfigurable {
    IReader ReaderElement;
    IExecutor ExecutorElement;
    IWriter WriterElement;
    boolean IsInited;
    FileInputStream FileIn;
    FileOutputStream FileOut;
    // custom return codes
    public static final RC RC_MANAGER_NOT_INITED = new RC(RC.RCWho.MANAGER, RC.RCType.CODE_CUSTOM_ERROR, "Class not inited");

    public static boolean handleRC(RC returnCode)
    {
        if (!returnCode.isSuccess())
        {
            System.out.println("ERROR: " + returnCode.who.get() + ": " + returnCode.info);
            return false;
        }
        return true;
    }

    public Manager()
    {
        IsInited = false;
    }

    public RC setConfig(String var1)
    {
        ManagerConfig config = new ManagerConfig();
        RC code = config.process(var1);
        if (code.isSuccess())
        {
            // open files
            try {
                FileIn = new FileInputStream(config.GetField(ManagerConfig.ConfigFields.IN_FILENAME));
            } catch (FileNotFoundException e) {
                return RC.RC_MANAGER_INVALID_INPUT_FILE;
            }

            try {
                FileOut = new FileOutputStream(config.GetField(ManagerConfig.ConfigFields.OUT_FILENAME));
            } catch (FileNotFoundException e) {
                return RC.RC_MANAGER_INVALID_OUTPUT_FILE;
            }

            // Get all classes
            try {
                Class<?> reader = Class.forName(config.GetField(ManagerConfig.ConfigFields.READER_NAME));
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

            try {
                Class<?> executor = Class.forName(config.GetField(ManagerConfig.ConfigFields.EXECUTOR_NAME));
                if (IExecutor.class.isAssignableFrom(executor)) {
                    ExecutorElement = (IExecutor) executor.getDeclaredConstructor().newInstance();
                }
                else {
                    return RC.RC_MANAGER_INVALID_EXECUTOR_CLASS;
                }
            }
            catch (Exception e) {
                return RC.RC_MANAGER_INVALID_EXECUTOR_CLASS;
            }

            try {
                Class<?> writer = Class.forName(config.GetField(ManagerConfig.ConfigFields.WRITER_NAME));
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
            tmp = ReaderElement.setConfig(config.GetField(ManagerConfig.ConfigFields.READER_CONFIG));
            if (!tmp.isSuccess())
                return tmp;

            tmp = ExecutorElement.setConfig(config.GetField(ManagerConfig.ConfigFields.EXECUTOR_CONFIG));
            if (!tmp.isSuccess())
                return tmp;

            tmp = WriterElement.setConfig(config.GetField(ManagerConfig.ConfigFields.WRITER_CONFIG));
            if (!tmp.isSuccess())
                return tmp;

            // place them with all other initing

            tmp = ReaderElement.setInputStream(FileIn);
            if (!tmp.isSuccess())
                return tmp;

            tmp = ReaderElement.setConsumer(ExecutorElement);
            if (!tmp.isSuccess())
                return tmp;

            tmp = ExecutorElement.setConsumer(WriterElement);
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

    public static void main(String[] args) {
        if (args.length != 1) {
            handleRC(RC_MANAGER_INVALID_ARGUMENT);
            return;
        }

        Manager manager = new Manager();
        if (handleRC(manager.setConfig(args[0]))) {
            if (handleRC(manager.execute())) {
                System.out.println("Everything succeed!");
            }
        }
    }
}
