
public class Main {
    public static void main(String[] args) {
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