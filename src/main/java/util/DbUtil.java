package util;

import config.AllNodeCommonMsg;
import dao.bean.DbDao;
import dao.pbft.MsgCollection;
import dao.pbft.PBFTMsg;
import lombok.extern.slf4j.Slf4j;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.iq80.leveldb.impl.Iq80DBFactory.factory;

@Slf4j
public class DbUtil {
    public static String dbFilePath;

    private static DB db = null;
    private static Options options = new Options();
    private static boolean flag = true;

    private static boolean init() {
        options = new Options();
        options.createIfMissing(true);
        try {
            db = factory.open(new File(dbFilePath), options);
        } catch (IOException e) {
            log.warn(String.format("%s", e.getMessage()));
            return false;
        }
        return true;
    }

    private static void insert(DbDao dao) {
        try {
            db.put(String.valueOf(dao.getNode()).getBytes(), daoToBytes(dao));
        } catch (IOException e) {
            log.warn(String.format("%s", e.getMessage()));
        }
    }

    synchronized public static void save() {
        if (!flag) {
            return;
        }
        flag = false;
        log.info(String.format("%s", MsgCollection.getInstance().getDbDaos().size()));
        if (init()) {
            for (DbDao dao :
                    MsgCollection.getInstance().getDbDaos()) {
                insert(dao);
            }
            try {
                db.close();
            } catch (IOException e) {
                log.warn(String.format("%s", e.getMessage()));
            }
        }

    }

    private static void get() {
        Options options = new Options();
        options.createIfMissing(true);
        DB db = null;
        try {
            db = factory.open(new File(dbFilePath), options);
        } catch (IOException e) {
            log.warn(String.format("%s", e.getMessage()));
            return;
        }
        DBIterator iterator = db.iterator();
        List<byte[]> list = new ArrayList<byte[]>();
        while (iterator.hasNext()) {
            Map.Entry<byte[], byte[]> next = iterator.next();
            byte[] value = next.getValue();
            list.add(value);
        }
        System.out.println(list.size());
        for (byte[] bytes : list) {
            try {
                DbDao dbDao = (DbDao) bytesToDao(bytes);
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

    }

    public static void addDaotoList(int node, PBFTMsg msg) {
        DbDao dbDao = new DbDao();
        dbDao.setNode(node);
        //dbDao.setPublicKey(AllNodeCommonMsg.publicKeyMap.get(node));
        dbDao.setTime(msg.getTime());
        dbDao.setViewNum(msg.getViewNum());
        MsgCollection.getInstance().getDbDaos().add(dbDao);
    }

    private static byte[] daoToBytes(DbDao dao) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(dao);
        oos.writeObject(null);
        oos.close();
        baos.close();
        return baos.toByteArray();
    }

    private static Object bytesToDao(byte[] bytes) throws IOException, ClassNotFoundException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
        ObjectInputStream inputStream = new ObjectInputStream(byteArrayInputStream);
        Object obj = null;
        while ((obj = inputStream.readObject()) != null) {
            DbDao dbDao = (DbDao) obj;
            System.out.println(dbDao);
        }
        inputStream.close();
        byteArrayInputStream.close();
        return obj;
    }
}
