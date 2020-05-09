public class OFT {
    private PackableMemory buffer;
    private int position;
    private int size;
    private int descriptor;

    public OFT(int pos) {
        buffer = new PackableMemory(512);
        position = (pos == 0 ? pos : -1);
        size = 0;
        descriptor = 0;
    }

    public PackableMemory getBuffer() {
        return buffer;
    }

    public int getPosition() {
        return position;
    }

    public int getSize() {
        return size;
    }

    public int getDescriptor() {
        return descriptor;
    }

    public void setBuffer(PackableMemory buf) {
        buffer = buf;
    }

    public void setPosition(int pos) {
        position = pos;
    }

    public void setBufferAtPos(int pos, byte val) {
        buffer.mem[pos] = val;
    }

    public void setSize(int s) {
        size = s;
    }

    public void setDescriptor(int desc) {
        descriptor = desc;
    }
}
