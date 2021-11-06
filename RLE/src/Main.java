enum RC {
    ERROR_NO_ERROR("",""),
    ERROR_INPUT_ERROR("IN", ""),
    ERROR_OUTPUT_ERROR("OUT", "");

    String prefix;
    String message;

    RC(String inPref, String inPost)
    {
        prefix = inPref;
        message = inPost;
    }

    void SetMsg(String msg)
    {
        message = msg;
    }
}

public class Main {
    public static void main(String[] args) {
        RC A = RC.ERROR_NO_ERROR;
        RC B = RC.ERROR_OUTPUT_ERROR;
        B.SetMsg("SuperMessage");
        RC C = RC.ERROR_OUTPUT_ERROR;


        if (args.length != 1) {
            System.out.println("Usage RLE.jar <config_name.txt>");
        }
        else {
            Config conf = new Config(args[0]);
            RLE compressor = new RLE(conf);
            compressor.Run();
        }
    }
}
