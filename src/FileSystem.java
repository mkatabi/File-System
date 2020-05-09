import java.util.ArrayList;

public class FileSystem {
    private ArrayList<PackableMemory> D;
    private OFT[] OFT;
    PackableMemory I;
    PackableMemory O;
    PackableMemory M;

    public FileSystem() {
        initFS();
    }

    public ArrayList<PackableMemory> getDisk() {
        return D;
    }

    public OFT[] getOFT() {
        return OFT;
    }

    public PackableMemory getInputBuffer() {
        return I;
    }

    public PackableMemory getOutputBuffer() {
        return O;
    }

    public PackableMemory getMemoryBuffer() {
        return M;
    }

    public void init() {
        initFS();
        System.out.println("system initialized");
    }

    public void seek(int i, int p) {
        int res = seekFS(i, p);

        if (res == -1)
            System.out.println("error");
        else
            System.out.println("position is " + p);
    }

    public void create(String name) {
        seekFS(0, 0);
        byte[] buffer = OFT[0].getBuffer().mem;

        // check if file already exists
        for (int i = 0; i < 64; i++) {
            for (int j = 0; j < name.length(); j++) {
                int ind = i * 8 + j;
                char letter = (char)buffer[ind];
                if (letter != name.charAt(j))
                    break;
                else if (j == name.length()-1 && buffer[ind] == name.charAt(j)) {
                    System.out.println("error");
                    return;
                }
            }
        }

        // search for free file descriptor
        int freeFD = -1;
        for (int i = 1; i < 7; i++) {
            for (int j = 0; j < 32; j++) {
                int size = D.get(i).unpack(j*16);
                if (size == -1) {
                    freeFD = j * 16;
                    D.get(i).pack(0, j*16);
                    break;
                }
            }
            if (freeFD != -1)
                break;
        }
        if (freeFD == -1) {
            System.out.println("error");
            return;
        }

        // search for a free directory entry
        seekFS(0, 0);
        for (int i = 0; i < 64; i++) {
            if (OFT[0].getBuffer().mem[i*8] == 0) {
                for (int j = 0; j < name.length(); j++) {
                    int ind = i * 8 + j;
                    byte letter = (byte) name.charAt(j);
                    OFT[0].setBufferAtPos(ind, letter);
                }

                OFT[0].getBuffer().pack(freeFD, i*8+4);
                System.out.println(name + " created");
                return;
            }
        }

        System.out.println("error");
        return;
    }

    public void destroy(String name) {
        int fd = -1, nameInd = -1;
        seekFS(0, 0);
        byte[] buffer = OFT[0].getBuffer().mem;

        // if name field matches name, then get its descriptor index
        for (int i = 0; i < 64; i++) {
            for (int j = 0; j < name.length(); j++) {
                int ind = i * 8 + j;
                char letter = (char)buffer[ind];
                if (letter != name.charAt(j))
                    break;
                else if (j == name.length()-1 && buffer[ind] == name.charAt(j)) {
                    nameInd = i * 8;
                    fd = OFT[0].getBuffer().unpack(i * 8 + 4);
                    OFT[0].getBuffer().pack(0, i * 8 + 4);
                    break;
                }
            }
            if (fd != -1)
                break;
        }

        // if end of file is reached, exit with error: file does not exist
        if (fd == -1) {
            System.out.println("error");
            return;
        }

        // check if file to be destroyed in OFT; remove file from OFT if true
        for (int i = 1; i < OFT.length; i++) {
            if (OFT[i].getDescriptor() == fd) {
                OFT[i] = new OFT(i);
                break;
            }
        }

        int block = (fd / 512) + 1;
        int fdIndex = fd % 512;

        D.get(block).pack(-1, fdIndex);   // mark descriptor i as free by setting size field to -1

        // set all block numbers to 0 and update bitmap to free block for each nonzero block
        for (int i = 1; i < 4; i++) {
            int blockNum = D.get(block).unpack(fdIndex + (4*i));
            if (blockNum != -1)
                D.get(0).pack(0, blockNum * 4);   // for each nonzero block in descriptor, update bitmap to free block
            D.get(block).pack(-1, fdIndex + (4*i));   // set all block numbers to -1
        }

        for (int i = nameInd; i < name.length() + nameInd; i++)
            OFT[0].setBufferAtPos(i, (byte)0);   // mark directory entry as free by setting name field to 0

        System.out.println(name + " destroyed");
    }

    public void open(String name) {
        int fd = -1;
        byte[] buffer = OFT[0].getBuffer().mem;

        // search directory for a match on file name
        for (int i = 0; i < 64; i++) {
            for (int j = 0; j < name.length(); j++) {
                int ind = i * 8 + j;
                char letter = (char)buffer[ind];
                if (letter != name.charAt(j))
                    break;
                else if (j == name.length()-1 && buffer[ind] == name.charAt(j)) {
                    fd = OFT[0].getBuffer().unpack(i * 8 + 4);
                    break;
                }
            }
            if (fd != -1)
                break;
        }

        // if no entry found, exit with error: file does not exist
        if (fd == -1) {
            System.out.println("error");
            return;
        }

        // search OFT to see if file is already opened
        for (int i = 1; i < OFT.length; i++) {
            if (OFT[i].getDescriptor() == fd) {
                System.out.println("error");
                return;
            }
        }

        // search for free OFT entry
        int freeOFT = -1;
        for (int i = 1; i < OFT.length; i++) {
            if (OFT[i].getPosition() == -1) {
                freeOFT = i;
                OFT[i].setPosition(0);   // enter 0 into current position of free OFT entry found
                break;
            }
        }

        // if no entry found, exit with error: too many files open
        if (freeOFT == -1) {
            System.out.println("error");
            return;
        }

        int block = (fd / 512) + 1;
        int fdIndex = fd % 512;
        int size = D.get(block).unpack(fdIndex);
        OFT[freeOFT].setSize(size);   // copy file size from file descriptor into OFT entry
        OFT[freeOFT].setDescriptor(fd);   // enter file descriptor index into file descriptor field of OFT entry

        // if file size = 0, use bitmap to find free block, record block number in descriptor
        if (size == 0) {
            int freeBlock = -1;
            PackableMemory bitmap = D.get(0);
            for (int i = 0; i < 64; i++) {
                int bit = bitmap.unpack(i*4);
                if (bit == 0) {
                    freeBlock = i;
                    bitmap.pack(1, i*4);
                    break;
                }
            }
            D.get(block).pack(freeBlock, fdIndex+4);
        }
        // otherwise, copy first block of file into the buffer of OFT entry
        else {
            int openBlock = D.get(block).unpack(fdIndex+4);
            OFT[freeOFT].setBuffer(D.get(openBlock));
        }

        System.out.println(name + " opened " + freeOFT);
    }

    public void close(int index) {
        if (OFT[index].getPosition() == -1) {
            System.out.println("error");
            return;
        }

        // write buffer to disk
        int f = OFT[index].getDescriptor();
        int fBlock = f / 512 + 1;
        int fIndex = f % 512;
        int blockPlace = OFT[index].getSize() / 512;
        int blockBuff = D.get(fBlock).unpack(fIndex + (4*(blockPlace+1)));   // determine (using current position) which block is currently in buffer
        D.set(blockBuff, OFT[index].getBuffer());   // copy buffer contents to disk block

        // update file size in descriptor
        int fileSize = OFT[index].getSize();
        int fd = OFT[index].getDescriptor();
        int fdBlock = (fd / 512) + 1;
        D.get(fdBlock).pack(fileSize, fd % 512);   // copy file size from OFT to descriptor

        // mark OFT entry as free
        OFT[index].setPosition(-1);   // set current position to -1
        OFT[index] = new OFT(index);

        System.out.println(index + " closed");
    }

    public void directory() {
        seekFS(0, 0);   // seek to position 0 in directory
        byte[] buffer = OFT[0].getBuffer().mem;

        // repeat until end of file is reached
        for (int i = 0; i < 64; i++) {
            String name = "";
            for (int j = 0; j < 4; j++) {
                if (buffer[i*8+j] != 0)
                    name += (char)buffer[i*8+j];   // get file name
            }
            // if name field is not 0
            if (!name.equals("")) {
                int fd = OFT[0].getBuffer().unpack(i*8+4);   // find file descriptor
                int fdBlock = (fd / 512) + 1;
                int fdIndex = fd % 512;
                int fileSize = D.get(fdBlock).unpack(fdIndex);   // get file size
                System.out.print(name + " " + fileSize + " ");
            }
        }

        System.out.println();
    }

    public void read(int i, int m, int n) {
        if (OFT[i].getPosition() == -1) {
            System.out.println("error");
            return;
        }

        // copy bytes from buffer to memory
        int bytesCopied = 0, memoryInd = m, fileSize = OFT[i].getSize();
        while (bytesCopied < n && OFT[i].getPosition() < fileSize) {
            int pos = OFT[i].getPosition();

            int buf;
            if (pos == 512 || pos == 1024 || pos == 1536)
                buf = pos/512;
            else
                buf = (pos / 512) + 1;

            int offset = pos % 512;
            // end of buffer is reached
            if (pos != 0 && offset == 0) {
                int fd = OFT[i].getDescriptor();
                int fdBlock = fd / 512 + 1;
                int fdIndex = fd % 512;
                int curBlock = D.get(fdBlock).unpack(fdIndex + (4*buf));
                D.set(curBlock, OFT[i].getBuffer());
                int nextBlock = D.get(fdBlock).unpack(fdIndex + (4*(buf+1)));
                OFT[i].setBuffer(D.get(nextBlock));
            }

            memoryInd = memoryInd % 512;
            M.mem[memoryInd] = OFT[i].getBuffer().mem[offset];
            memoryInd++;
            OFT[i].setPosition(++pos);
            bytesCopied++;
        }

        System.out.println(bytesCopied + " bytes read from " + i);
    }

    public void write(int i, int m, int n) {
        int bytesCopied = 0, memoryInd = m;

        if (OFT[i].getPosition() == -1) {
            System.out.println("error");
            return;
        }

        while (bytesCopied < n && OFT[i].getPosition() < 1536) {
            int pos = OFT[i].getPosition();

            int buf;
            if (pos == 512 || pos == 1024 || pos == 1536)
                buf = pos/512;
            else
                buf = (pos / 512) + 1;

            int offset = pos % 512;
            int fd = OFT[i].getDescriptor();
            int fdBlock = (fd / 512) + 1;
            int fdIndex = fd % 512;
            // end of buffer is reached
            if (pos != 0 && offset == 0) {
                int curBlock = D.get(fdBlock).unpack(fdIndex + (4*buf));
                D.set(curBlock, OFT[i].getBuffer());
                int nextBlock = D.get(fdBlock).unpack(fdIndex + (4*(buf+1)));
                if (nextBlock != -1)
                    OFT[i].setBuffer(D.get(nextBlock));
                else {
                    int freeBlock = -1;
                    PackableMemory bitmap = D.get(0);
                    for (int j = 0; j < 64; j++) {
                        int bit = bitmap.unpack(j*4);
                        if (bit == 0) {
                            freeBlock = j;
                            bitmap.pack(1, j*4);
                            break;
                        }
                    }
                    D.get(fdBlock).pack(freeBlock, fdIndex + (4*(buf+1)));
                }
            }

            OFT[i].setBufferAtPos(offset, M.mem[memoryInd++]);
            OFT[i].setPosition(++pos);
            bytesCopied++;

            if (pos > OFT[i].getSize()) {
                OFT[i].setSize(pos);
                D.get(fdBlock).pack(pos, fdIndex);
            }
        }

        System.out.println(bytesCopied + " bytes written to " + i);
    }

    public void readMemory(int m, int n) {
        for (int i = m; i < m+n; i++)
            System.out.print((char)M.mem[i]);

        System.out.println();
    }

    public void writeMemory(int m, String s) {
        int j = 0;
        for (int i = m; j < s.length(); i++)
            M.mem[i] = (byte)s.charAt(j++);

        System.out.println(s.length() + " bytes written to M");
    }

    private PackableMemory initBitmap() {
        PackableMemory pm = new PackableMemory(512);

        for (int i = 0; i < 64; i++)
            if (i >= 0 && i <= 7 )
                pm.pack(1, i*4);
            else
                pm.pack(0, i*4);

        return pm;
    }

    private PackableMemory initFileDescriptor(int block) {
        PackableMemory pm = new PackableMemory(512);

        for (int i = 0; i < 128; i+=4) {
            if (block == 0 && i == 0) {
                pm.pack(0, i*4);   // size
                pm.pack(7, i*4+4);   // first block index
            }
            else {
                pm.pack(-1, i*4);   // size
                pm.pack(-1, i*4+4);   // first block index
            }

            pm.pack(-1, i*4+8);   // second block index
            pm.pack(-1, i*4+12);   // third block index
        }

        return pm;
    }

    private void readBlock(int b) {
        I = D.get(b);
    }

    private void writeBlock(int b) {
        D.set(b, O);
    }

    private void initFS() {
        D = new ArrayList<>(64);
        D.add(initBitmap());

        for (int i = 0; i < 6; i++)
            D.add(initFileDescriptor(i));

        for (int i = 0; i < 57; i++)
            D.add(new PackableMemory(512));

        OFT = new OFT[4];
        for (int i = 0; i < OFT.length; i++)
            OFT[i] = new OFT(i);

        I = new PackableMemory(512);
        O = new PackableMemory(512);
        M = new PackableMemory(512);
    }

    private int seekFS(int i, int p) {
        OFT file = OFT[i];

        // if p > file size, exit with error
        if (p > file.getSize()) {
            return -1;
        }

        int desc = file.getDescriptor();
        int blockInd;
        if (file.getPosition() == 512 || file.getPosition() == 1024 || file.getPosition() == 1536)
            blockInd = file.getPosition() / 512;
        else
            blockInd = (file.getPosition() / 512) + 1;
        int fdBlock = desc / 512 + 1;
        int fdIndex = desc % 512;
        int currentBlock = D.get(fdBlock).unpack(fdIndex + (4*blockInd));

        int block1;
        if (file.getPosition() == 512 || file.getPosition() == 1024 || file.getPosition() == 1536)
            block1 = file.getPosition() / 512;
        else
            block1 = file.getPosition() / 512 + 1;
        int block2;
        if (p == 512 || p == 1024 || p == 1536)
            block2 = p / 512;
        else
            block2 = p / 512 + 1;

        int newBlock = D.get(fdBlock).unpack(fdIndex + (4*(block2)));

        // if buffer does not currently contain new block
        if (block1 != block2) {
            D.set(currentBlock, OFT[i].getBuffer());
            OFT[i].setBuffer(D.get(newBlock));
        }

        // set current position to p
        file.setPosition(p);
        return 1;
    }
}