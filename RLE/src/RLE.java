public class RLE {
    public RLE(Config config) {
        my_config = config;
    }

    public void Run(){
        if (!my_config.correct_config) {
            System.out.println("Incorrect Config. Won't run this");
            return;
        }
    }

    private void Compress() {

    }

    private void Decompress() {

    }

    private Config my_config;
}
