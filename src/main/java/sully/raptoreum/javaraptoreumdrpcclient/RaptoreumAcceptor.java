/*
 * RaptoreumdRpcClient-JSON-RPC-Client License
 * 
 * Copyright (c) 2013, Mikhail Yevchenko.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the 
 * Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject
 * to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR
 * ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH
 * THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package sully.raptoreum.javaraptoreumdrpcclient;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RaptoreumAcceptor implements Runnable {
    
    private static final Logger logger = Logger.getLogger(RaptoreumAcceptor.class.getCanonicalName());

    public final RaptoreumdRpcClient bitcoin;
    private String lastBlock, monitorBlock = null;
    int monitorDepth;
    private final LinkedHashSet<RaptoreumPaymentListener> listeners = new LinkedHashSet<RaptoreumPaymentListener>();

    public RaptoreumAcceptor(RaptoreumdRpcClient bitcoin, String lastBlock, int monitorDepth) {
        this.bitcoin = bitcoin;
        this.lastBlock = lastBlock;
        this.monitorDepth = monitorDepth;
    }
    
    public RaptoreumAcceptor(RaptoreumdRpcClient bitcoin) {
        this(bitcoin, null, 6);
    }

    public RaptoreumAcceptor(RaptoreumdRpcClient bitcoin, String lastBlock, int monitorDepth, RaptoreumPaymentListener listener) {
        this(bitcoin, lastBlock, monitorDepth);
        listeners.add(listener);
    }

    public RaptoreumAcceptor(RaptoreumdRpcClient bitcoin, RaptoreumPaymentListener listener) {
        this(bitcoin, null, 12);
        listeners.add(listener);
    }

    @SuppressWarnings("deprecation")
	public String getAccountAddress(String account) throws GenericRpcException {
        List<String> a = bitcoin.getAddressesByAccount(account);
        if (a.isEmpty())
            return bitcoin.getNewAddress(account);
        return a.get(0);
    }

    public synchronized String getLastBlock() {
        return lastBlock;
    }

    public synchronized void setLastBlock(String lastBlock) throws GenericRpcException {
        if (this.lastBlock != null)
            throw new IllegalStateException("lastBlock already set");
        this.lastBlock = lastBlock;
        updateMonitorBlock();
    }

    public synchronized RaptoreumPaymentListener[] getListeners() {
        return listeners.toArray(new RaptoreumPaymentListener[0]);
    }

    public synchronized void addListener(RaptoreumPaymentListener listener) {
        listeners.add(listener);
    }

    public synchronized void removeListener(RaptoreumPaymentListener listener) {
        listeners.remove(listener);
    }

    private HashSet<String> seen = new HashSet<String>();

    private void updateMonitorBlock() throws GenericRpcException {
        monitorBlock = lastBlock;
        for(int i = 0; i < monitorDepth && monitorBlock != null; i++) {
            RaptoreumdRpcClient.Block b = bitcoin.getBlock(monitorBlock);
            monitorBlock = b == null ? null : b.previousHash();
        }
    }

    public synchronized void checkPayments() throws GenericRpcException {
        RaptoreumdRpcClient.TransactionsSinceBlock t = monitorBlock == null ? bitcoin.listSinceBlock() : bitcoin.listSinceBlock(monitorBlock);
        for (RaptoreumdRpcClient.Transaction transaction : t.transactions()) {
            if ("receive".equals(transaction.category())) {
                if (!seen.add(transaction.txId()))
                    continue;
                for (RaptoreumPaymentListener listener : listeners) {
                    try {
                        listener.transaction(transaction);
                    } catch (Exception ex) {
                        logger.log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
        if (!t.lastBlock().equals(lastBlock)) {
            seen.clear();
            lastBlock = t.lastBlock();
            updateMonitorBlock();
            for (RaptoreumPaymentListener listener : listeners) {
                try {
                    listener.block(lastBlock);
                } catch (Exception ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    private boolean stop = false;
    
    public void stopAccepting() {
        stop = true;
    }
    
    private long checkInterval = 5000;

    /**
     * Get the value of checkInterval
     *
     * @return the value of checkInterval
     */
    public long getCheckInterval() {
        return checkInterval;
    }

    /**
     * Set the value of checkInterval
     *
     * @param checkInterval new value of checkInterval
     */
    public void setCheckInterval(long checkInterval) {
        this.checkInterval = checkInterval;
    }

    @Override
    public void run() {
        stop = false;
        long nextCheck = 0;
        while(!(Thread.interrupted() || stop)) {
            if (nextCheck <= System.currentTimeMillis())
                try {
                    nextCheck = System.currentTimeMillis() + checkInterval;
                    checkPayments();
                } catch (GenericRpcException ex) {
                    Logger.getLogger(RaptoreumAcceptor.class.getName()).log(Level.SEVERE, null, ex);
                }
            else
                try {
                    Thread.sleep(Math.max(nextCheck - System.currentTimeMillis(), 100));
                } catch (InterruptedException ex) {
                    Logger.getLogger(RaptoreumAcceptor.class.getName()).log(Level.WARNING, null, ex);
                }
        }
    }

//    public static void main(String[] args) {
//        //System.out.println(System.getProperties().toString().replace(", ", ",\n"));
//        final RaptoreumdRpcClient bitcoin = new BitcoinJSONRPCClient(true);
//        new RaptoreumAcceptor(bitcoin, null, 6, new BitcoinPaymentListener() {
//
//            public void block(String blockHash) {
//                try {
//                    System.out.println("new block: " + blockHash + "; date: " + bitcoin.getBlock(blockHash).time());
//                } catch (BitcoinRpcException ex) {
//                    logger.log(Level.SEVERE, null, ex);
//                }
//            }
//
//            public void transaction(Transaction transaction) {
//                System.out.println("tx: " + transaction.confirmations() + "\t" + transaction.amount() + "\t=> " + transaction.account());
//            }
//        }).run();
//    }

}
