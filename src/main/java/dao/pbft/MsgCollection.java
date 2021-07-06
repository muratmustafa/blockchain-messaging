package dao.pbft;

import dao.bean.DbDao;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.AtomicLongMap;
import lombok.Data;

import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

@Data
public class MsgCollection {

    private static MsgCollection msgCollection = new MsgCollection();

    private MsgCollection() {
    }

    public static MsgCollection getInstance() {
        return msgCollection;
    }

    private BlockingQueue<PBFTMsg> msgQueue = new LinkedBlockingQueue<PBFTMsg>();

    private AtomicLongMap<Integer> viewNumCount = AtomicLongMap.create();

    private CopyOnWriteArrayList<DbDao> dbDaos = new CopyOnWriteArrayList<DbDao>();

    private AtomicLong disagreeViewNum = new AtomicLong();

    private Set<String> votePrePrepare = Sets.newConcurrentHashSet();

    private AtomicLongMap<String> agreePrepare = AtomicLongMap.create();

    private AtomicLongMap<String> agreeCommit = AtomicLongMap.create();

}
