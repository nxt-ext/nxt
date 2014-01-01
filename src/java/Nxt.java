import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Nxt extends HttpServlet {
	
	static final String VERSION = "0.4.9e";
	
	static final long GENESIS_BLOCK_ID = 2680262203532249785L;
	static final long CREATOR_ID = 1739068987193023818L;
	static final int BLOCK_HEADER_LENGTH = 224;
	static final int MAX_PAYLOAD_LENGTH = 255 * 128;
	
	static final int ALIAS_SYSTEM_BLOCK = 22000;
	static final int TRANSPARENT_FORGING_BLOCK = 30000;

    static final long MAX_BALANCE = 1000000000;
	static final long initialBaseTarget = 153722867, maxBaseTarget = MAX_BALANCE * initialBaseTarget;
	static final BigInteger two64 = new BigInteger("18446744073709551616");

    // /*final*/ variables are set in the init() and are to be treated as final
	static /*final*/ long epochBeginning;
	static final String alphabet = "0123456789abcdefghijklmnopqrstuvwxyz";

    // blockchain.nrs not used anymore?
	//static FileChannel blockchainChannel;
	static /*final*/ String myPlatform, myScheme, myAddress, myHallmark;
	static /*final*/ int myPort;
	static /*final*/ boolean shareMyAddress;
	static /*final*/ Set<String> allowedUserHosts, allowedBotHosts;
	static /*final*/ int blacklistingPeriod;
	
	static final int LOGGING_MASK_EXCEPTIONS = 1;
	static final int LOGGING_MASK_NON200_RESPONSES = 2;
	static final int LOGGING_MASK_200_RESPONSES = 4;
	static /*final*/ int communicationLoggingMask;
	
	static final AtomicInteger transactionCounter = new AtomicInteger();
	static final ConcurrentMap<Long, Transaction> transactions = new ConcurrentHashMap<>();
	static final ConcurrentMap<Long, Transaction> unconfirmedTransactions = new ConcurrentHashMap<>();
    static final ConcurrentMap<Long, Transaction> doubleSpendingTransactions = new ConcurrentHashMap<>();
	
	static /*final*/ Set<String> wellKnownPeers;
	static /*final*/ int maxNumberOfConnectedPublicPeers;
	static /*final*/ int connectTimeout, readTimeout;
	static /*final*/ boolean enableHallmarkProtection;
	static /*final*/ int pushThreshold, pullThreshold;
	static final AtomicInteger peerCounter = new AtomicInteger();
	static final ConcurrentMap<String, Peer> peers = new ConcurrentHashMap<>();

    static final Object blocksAndTransactionsLock = new Object();

	static final AtomicInteger blockCounter = new AtomicInteger();
	static final ConcurrentMap<Long, Block> blocks = new ConcurrentHashMap<>();
    static volatile long lastBlock;
	static volatile Peer lastBlockchainFeeder;
	
	static final ConcurrentMap<Long, Account> accounts = new ConcurrentHashMap<>();
	
	static final ConcurrentMap<String, Alias> aliases = new ConcurrentHashMap<>();
	static final ConcurrentMap<Long, Alias> aliasIdToAliasMappings = new ConcurrentHashMap<>();
    //TODO: haven't looked at assets code yet
	static final HashMap<Long, Asset> assets = new HashMap<>();
	static final HashMap<String, Long> assetNameToIdMappings = new HashMap<>();
	
	static final HashMap<Long, AskOrder> askOrders = new HashMap<>();
	static final HashMap<Long, BidOrder> bidOrders = new HashMap<>();
	static final HashMap<Long, TreeSet<AskOrder>> sortedAskOrders = new HashMap<>();
	static final HashMap<Long, TreeSet<BidOrder>> sortedBidOrders = new HashMap<>();
	
	static final ConcurrentMap<String, User> users = new ConcurrentHashMap<>();
	static final ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(7);
	
	static final ConcurrentMap<Account, Block> lastBlocks = new ConcurrentHashMap<>();
	static final ConcurrentMap<Account, BigInteger> hits = new ConcurrentHashMap<>();
	
	static int getEpochTime(long time) {
		
		return (int)((time - epochBeginning + 500) / 1000);
		
	}

    static final ThreadLocal<SimpleDateFormat> logDateFormat = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss.SSS] ");
        }
    };

	static void logMessage(String message) {
		
		System.out.println(logDateFormat.get().format(new Date()) + message);

	}
	
	static byte[] convert(String string) {
		
		byte[] bytes = new byte[string.length() / 2];
		for (int i = 0; i < bytes.length; i++) {
			
			bytes[i] = (byte)Integer.parseInt(string.substring(i * 2, i * 2 + 2), 16);
			
		}
		
		return bytes;
		
	}
	
	static String convert(byte[] bytes) {
		
		StringBuilder string = new StringBuilder();
		for (int i = 0; i < bytes.length; i++) {
			
			int number;
			string.append(alphabet.charAt((number = bytes[i] & 0xFF) >> 4)).append(alphabet.charAt(number & 0xF));
			
		}
		
		return string.toString();
		
	}
	
	static String convert(long objectId) {
		
		BigInteger id = BigInteger.valueOf(objectId);
		if (objectId < 0) {
			
			id = id.add(two64);
			
		}
		
		return id.toString();
		
	}
	
	static void matchOrders(long assetId) throws Exception {
		
		TreeSet<AskOrder> sortedAskOrders = Nxt.sortedAskOrders.get(assetId);
		TreeSet<BidOrder> sortedBidOrders = Nxt.sortedBidOrders.get(assetId);
		
		synchronized (askOrders) {
			
			synchronized (bidOrders) {
				
				while (!sortedAskOrders.isEmpty() && !sortedBidOrders.isEmpty()) {
					
					AskOrder askOrder = sortedAskOrders.first();
					BidOrder bidOrder = sortedBidOrders.first();
					
					if (askOrder.price > bidOrder.price) {
						
						break;
						
					}
					
					int quantity = askOrder.quantity < bidOrder.quantity ? askOrder.quantity : bidOrder.quantity;
					long price = askOrder.height < bidOrder.height || (askOrder.height == bidOrder.height && askOrder.id < bidOrder.id) ? askOrder.price : bidOrder.price;
					
					if ((askOrder.quantity -= quantity) == 0) {
						
						askOrders.remove(askOrder.id);
						sortedAskOrders.remove(askOrder);
						
					}
					synchronized (askOrder.account) {
						
						askOrder.account.setBalance(askOrder.account.balance + quantity * price);
						askOrder.account.setUnconfirmedBalance(askOrder.account.unconfirmedBalance + quantity * price);
						
					}
					
					if ((bidOrder.quantity -= quantity) == 0) {
						
						bidOrders.remove(bidOrder.id);
						sortedBidOrders.remove(bidOrder);
						
					}
					synchronized (bidOrder.account) {
						
						Integer assetBalance = bidOrder.account.assetBalances.get(assetId);
						if (assetBalance == null) {
							
							bidOrder.account.assetBalances.put(assetId, quantity);
							bidOrder.account.unconfirmedAssetBalances.put(assetId, quantity);
							
						} else {
							
							bidOrder.account.assetBalances.put(assetId, assetBalance + quantity);
							bidOrder.account.unconfirmedAssetBalances.put(assetId, bidOrder.account.unconfirmedAssetBalances.get(assetId) + quantity);
							
						}
						
					}
					
				}
				
			}
			
		}
		
	}
	
	static class Account {
		
		final long id;
        //TODO: make balance AtomicLong?
		long balance;
		final int height;
		
        //BUG: this is accessed in multiple threads without being synchronized
        //TODO: use atomic variable
		byte[] publicKey;
		
		final HashMap<Long, Integer> assetBalances;
		
		long unconfirmedBalance;
		final HashMap<Long, Integer> unconfirmedAssetBalances;
		
		Account(long id) {
			
			this.id = id;
			height = Block.getLastBlock().height;
			
			assetBalances = new HashMap<>();
			unconfirmedAssetBalances = new HashMap<>();
			
		}
		
		static Account addAccount(long id) {

            Account account = new Account(id);
            accounts.put(id, account);

            return account;

		}
		
		void generateBlock(String secretPhrase) throws Exception {
			
			Transaction[] sortedTransactions;

            sortedTransactions = unconfirmedTransactions.values().toArray(new Transaction[0]);

            //TODO: can be simplified, if removing transactions with absent referencedTransaction is all this is indeed doing
            while (sortedTransactions.length > 0) {

                int i;
                for (i = 0; i < sortedTransactions.length; i++) {

                    Transaction transaction = sortedTransactions[i];
                    if (transaction.referencedTransaction != 0 && transactions.get(transaction.referencedTransaction) == null) {

                        sortedTransactions[i] = sortedTransactions[sortedTransactions.length - 1];
                        Transaction[] tmp = new Transaction[sortedTransactions.length - 1];
                        System.arraycopy(sortedTransactions, 0, tmp, 0, tmp.length);
                        sortedTransactions = tmp;

                        break;

                    }

                }
                if (i == sortedTransactions.length) {

                    break;

                }

            }

            Arrays.sort(sortedTransactions);
			
			HashMap<Long, Transaction> newTransactions = new HashMap<>();
			HashSet<String> newAliases = new HashSet<>();
			HashMap<Long, Long> accumulatedAmounts = new HashMap<>();
			int payloadLength = 0;

			while (payloadLength <= MAX_PAYLOAD_LENGTH) {
				
				int prevNumberOfNewTransactions = newTransactions.size();
				
				for (Transaction transaction : sortedTransactions) {
					
					int transactionLength = transaction.getBytes().length;
					if (newTransactions.get(transaction.getId()) == null && payloadLength + transactionLength <= MAX_PAYLOAD_LENGTH) {
						
						long sender = Account.getId(transaction.senderPublicKey);
						Long accumulatedAmount = accumulatedAmounts.get(sender);
						if (accumulatedAmount == null) {

							accumulatedAmount = 0L;
							
						}
						
						long amount = (transaction.amount + transaction.fee) * 100L;
                        //BUG: account.balance accessed without being synchronized on account
                        //TODO: add account.getBalance() method that takes care of synchronization and make account.balance private
						if (accumulatedAmount + amount <= accounts.get(sender).balance && transaction.validateAttachment()) {
							
							switch (transaction.type) {
							
							case Transaction.TYPE_MESSAGING:
								{
									
									switch (transaction.subtype) {
									
									case Transaction.SUBTYPE_MESSAGING_ALIAS_ASSIGNMENT:
										{
											
											if (!newAliases.add(((Transaction.MessagingAliasAssignmentAttachment)transaction.attachment).alias.toLowerCase())) {
												
												continue;
												
											}
											
										}
										break;
										
									}
									
								}
								break;
								
							}
							
							accumulatedAmounts.put(sender, accumulatedAmount + amount);
							
							newTransactions.put(transaction.getId(), transaction);
							payloadLength += transactionLength;
							
						}
						
					}
					
				}
				
				if (newTransactions.size() == prevNumberOfNewTransactions) {
					
					break;
					
				}
				
			}
			
			Block block;
			if (Block.getLastBlock().height < TRANSPARENT_FORGING_BLOCK) {
				
				block = new Block(1, getEpochTime(System.currentTimeMillis()), lastBlock, newTransactions.size(), 0, 0, 0, null, Crypto.getPublicKey(secretPhrase), null, new byte[64]);
				
			} else {
				
				byte[] previousBlockHash = MessageDigest.getInstance("SHA-256").digest(Block.getLastBlock().getBytes());
				block = new Block(2, getEpochTime(System.currentTimeMillis()), lastBlock, newTransactions.size(), 0, 0, 0, null, Crypto.getPublicKey(secretPhrase), null, new byte[64], previousBlockHash);
				
			}
			block.transactions = new long[block.numberOfTransactions];
			int i = 0;
			for (Map.Entry<Long, Transaction> transactionEntry : newTransactions.entrySet()) {
				
				Transaction transaction = transactionEntry.getValue();
				block.totalAmount += transaction.amount;
				block.totalFee += transaction.fee;
				block.payloadLength += transaction.getBytes().length;
				block.transactions[i++] = transactionEntry.getKey();
				
			}
			
			Arrays.sort(block.transactions);
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			for (i = 0; i < block.numberOfTransactions; i++) {
				
				digest.update(newTransactions.get(block.transactions[i]).getBytes());
				
			}
			block.payloadHash = digest.digest();
			
			if (Block.getLastBlock().height < TRANSPARENT_FORGING_BLOCK) {
				
				block.generationSignature = Crypto.sign(Block.getLastBlock().generationSignature, secretPhrase);
				
			} else {
				
				digest.update(Block.getLastBlock().generationSignature);
				block.generationSignature = digest.digest(Crypto.getPublicKey(secretPhrase));
				
			}
			
			byte[] data = block.getBytes();
			byte[] data2 = new byte[data.length - 64];
			System.arraycopy(data, 0, data2, 0, data2.length);
			block.blockSignature = Crypto.sign(data2, secretPhrase);

			if (block.verifyBlockSignature() && block.verifyGenerationSignature()) {

                JSONObject request = block.getJSONObject(newTransactions);
                request.put("requestType", "processBlock");
                Peer.sendToAllPeers(request);
				
			} else {
				
				logMessage("Generated an incorrect block. Waiting for the next one...");
				
			}
			
		}
		
		int getEffectiveBalance() {
			
			if (height == 0) {
				
				return (int)(balance / 100);
				
			}
			
			if (Block.getLastBlock().height - height < 1440) {
				
				return 0;
				
			}
			
			int amount = 0;
			for (long transactionId : Block.getLastBlock().transactions) {
				
				Transaction transaction = transactions.get(transactionId);
				if (transaction.recipient == id) {
					
					amount += transaction.amount;
					
				}
				
			}

			return (int)(balance / 100) - amount;
			
		}
		
		static long getId(byte[] publicKey) throws Exception {
			
			byte[] publicKeyHash = MessageDigest.getInstance("SHA-256").digest(publicKey);
			BigInteger bigInteger = new BigInteger(1, new byte[] {publicKeyHash[7], publicKeyHash[6], publicKeyHash[5], publicKeyHash[4], publicKeyHash[3], publicKeyHash[2], publicKeyHash[1], publicKeyHash[0]});
			return bigInteger.longValue();
			
		}
		
        //TODO: setBalance is in almost all case used together with setUnconfirmedBalance, within synchronized(account).
        // Consider adding a single method that sets both, and is synchronized
		void setBalance(long balance) throws Exception {
			
			this.balance = balance;
			
			for (Peer peer : peers.values()) {
				
				if (peer.accountId == id && peer.adjustedWeight > 0) {
					
					peer.updateWeight();
					
				}
				
			}
			
		}
		
		void setUnconfirmedBalance(long unconfirmedBalance) throws Exception {
			
			this.unconfirmedBalance = unconfirmedBalance;
			
			JSONObject response = new JSONObject();
			response.put("response", "setBalance");
			response.put("balance", unconfirmedBalance);
			
			for (User user : users.values()) {
				
				if (user.secretPhrase != null && Account.getId(Crypto.getPublicKey(user.secretPhrase)) == id) {
					
					user.send(response);
					
				}
				
			}
			
		}
		
	}
	
	static class Alias {
		
		Account account;
		String alias;
		String uri;
		int timestamp;
		
		Alias(Account account, String alias, String uri, int timestamp) {
			
			this.account = account;
			this.alias = alias;
			this.uri = uri;
			this.timestamp = timestamp;
			
		}
		
	}
	
	static class AskOrder implements Comparable<AskOrder> {
		
		long id;
		long height;
		Account account;
		long asset;
		int quantity;
		long price;
		
		AskOrder(long id, Account account, long asset, int quantity, long price) {
			
			this.id = id;
			height = Block.getLastBlock().height;
			this.account = account;
			this.asset = asset;
			this.quantity = quantity;
			this.price = price;
			
		}
		
		@Override
		public int compareTo(AskOrder o) {
			
			if (price < o.price) {
				
				return -1;
				
			} else if (price > o.price) {
				
				return 1;
				
			} else {
				
				if (height < o.height) {
					
					return -1;
					
				} else if (height > o.height) {
					
					return 1;
					
				} else {
					
					if (id < o.id) {
						
						return -1;
						
					} else if (id > o.id) {
						
						return 1;
						
					} else {
						
						return 0;
						
					}
					
				}
				
			}
			
		}
		
	}
	
	static class Asset {
		
		long accountId;
		String name, description;
		int quantity;
		
		Asset(long accountId, String name, String description, int quantity) {
			
			this.accountId = accountId;
			this.name = name;
			this.description = description;
			this.quantity = quantity;
			
		}
		
	}
	
	static class BidOrder implements Comparable<BidOrder> {
		
		long id;
		long height;
		Account account;
		long asset;
		int quantity;
		long price;
		
		BidOrder(long id, Account account, long asset, int quantity, long price) {
			
			this.id = id;
			height = Block.getLastBlock().height;
			this.account = account;
			this.asset = asset;
			this.quantity = quantity;
			this.price = price;
			
		}
		
		@Override
		public int compareTo(BidOrder o) {
			
			if (price > o.price) {
				
				return -1;
				
			} else if (price < o.price) {
				
				return 1;
				
			} else {
				
				if (height < o.height) {
					
					return -1;
					
				} else if (height > o.height) {
					
					return 1;
					
				} else {
					
					if (id < o.id) {
						
						return -1;
						
					} else if (id > o.id) {
						
						return 1;
						
					} else {
						
						return 0;
						
					}
					
				}
				
			}
			
		}
		
	}
	
	static class Block implements Serializable {
		
		static final long serialVersionUID = 0;
		
        //TODO: at least some of those should be volatile
		final int version;
		final int timestamp;
		final long previousBlock;
		final int numberOfTransactions;
		int totalAmount, totalFee;
		int payloadLength;
		byte[] payloadHash;
		final byte[] generatorPublicKey;
		byte[] generationSignature;
		byte[] blockSignature;
		
		final byte[] previousBlockHash;
		
		int index;
		long[] transactions;
		long baseTarget;
		int height;
		long nextBlock;
		BigInteger cumulativeDifficulty;

		Block(int version, int timestamp, long previousBlock, int numberOfTransactions, int totalAmount, int totalFee, int payloadLength, byte[] payloadHash, byte[] generatorPublicKey, byte[] generationSignature, byte[] blockSignature) {

            this(version, timestamp, previousBlock, numberOfTransactions, totalAmount, totalFee, payloadLength, payloadHash, generatorPublicKey, generationSignature, blockSignature, null);

		}
		
		Block(int version, int timestamp, long previousBlock, int numberOfTransactions, int totalAmount, int totalFee, int payloadLength, byte[] payloadHash, byte[] generatorPublicKey, byte[] generationSignature, byte[] blockSignature, byte[] previousBlockHash) {
			
			this.version = version;
			this.timestamp = timestamp;
			this.previousBlock = previousBlock;
			this.numberOfTransactions = numberOfTransactions;
			this.totalAmount = totalAmount;
			this.totalFee = totalFee;
			this.payloadLength = payloadLength;
			this.payloadHash = payloadHash;
			this.generatorPublicKey = generatorPublicKey;
			this.generationSignature = generationSignature;
			this.blockSignature = blockSignature;
			
			this.previousBlockHash = previousBlockHash;
			
		}
		
		void analyze() throws Exception {
			
			if (previousBlock == 0) {
				
				lastBlock = GENESIS_BLOCK_ID;
				blocks.put(lastBlock, this);
				baseTarget = initialBaseTarget;
				cumulativeDifficulty = BigInteger.ZERO;
				
				Account.addAccount(CREATOR_ID);
				
			} else {
				
				Block.getLastBlock().nextBlock = getId();
				
				height = Block.getLastBlock().height + 1;
				lastBlock = getId();
				blocks.put(lastBlock, this);
				baseTarget = Block.getBaseTarget();
				cumulativeDifficulty = blocks.get(previousBlock).cumulativeDifficulty.add(two64.divide(BigInteger.valueOf(baseTarget)));
				
				Account generatorAccount = accounts.get(Account.getId(generatorPublicKey));
				synchronized (generatorAccount) {
					
					generatorAccount.setBalance(generatorAccount.balance + totalFee * 100L);
					generatorAccount.setUnconfirmedBalance(generatorAccount.unconfirmedBalance + totalFee * 100L);
					
				}
				
			}

            for (int i = 0; i < numberOfTransactions; i++) {

                Transaction transaction = Nxt.transactions.get(transactions[i]);

                long sender = Account.getId(transaction.senderPublicKey);
                Account senderAccount = accounts.get(sender);
                synchronized (senderAccount) {

                    senderAccount.setBalance(senderAccount.balance - (transaction.amount + transaction.fee) * 100L);
                    senderAccount.setUnconfirmedBalance(senderAccount.unconfirmedBalance - (transaction.amount + transaction.fee) * 100L);

                    if (senderAccount.publicKey == null) {

                        senderAccount.publicKey = transaction.senderPublicKey;

                    }

                }

                Account recipientAccount = accounts.get(transaction.recipient);
                if (recipientAccount == null) {

                    recipientAccount = Account.addAccount(transaction.recipient);

                }

                //TODO: refactor, don't use switch but create e.g. transaction handler class for each case
                switch (transaction.type) {

                    case Transaction.TYPE_PAYMENT:
                    {

                        switch (transaction.subtype) {

                            case Transaction.SUBTYPE_PAYMENT_ORDINARY_PAYMENT:
                            {

                                synchronized (recipientAccount) {

                                    recipientAccount.setBalance(recipientAccount.balance + transaction.amount * 100L);
                                    recipientAccount.setUnconfirmedBalance(recipientAccount.unconfirmedBalance + transaction.amount * 100L);

                                }

                            }
                            break;

                        }

                    }
                    break;

                    case Transaction.TYPE_MESSAGING:
                    {

                        switch (transaction.subtype) {

                            case Transaction.SUBTYPE_MESSAGING_ALIAS_ASSIGNMENT:
                            {

                                Transaction.MessagingAliasAssignmentAttachment attachment = (Transaction.MessagingAliasAssignmentAttachment)transaction.attachment;

                                String normalizedAlias = attachment.alias.toLowerCase();

                                Alias alias = aliases.get(normalizedAlias);
                                if (alias == null) {

                                    alias = new Alias(senderAccount, attachment.alias, attachment.uri, timestamp);
                                    aliases.put(normalizedAlias, alias);
                                    aliasIdToAliasMappings.put(transaction.getId(), alias);

                                } else {

                                    alias.uri = attachment.uri;
                                    alias.timestamp = timestamp;

                                }

                            }
                            break;

                        }

                    }
                    break;

                    case Transaction.TYPE_COLORED_COINS:
                    {

                        switch (transaction.subtype) {

                            case Transaction.SUBTYPE_COLORED_COINS_ASSET_ISSUANCE:
                            {

                                Transaction.ColoredCoinsAssetIssuanceAttachment attachment = (Transaction.ColoredCoinsAssetIssuanceAttachment)transaction.attachment;

                                long assetId = transaction.getId();
                                Asset asset = new Asset(sender, attachment.name, attachment.description, attachment.quantity);
                                //TODO: use concurrent collections instead of synchronized
                                synchronized (assets) {

                                    assets.put(assetId, asset);
                                    assetNameToIdMappings.put(attachment.name.toLowerCase(), assetId);

                                }
                                synchronized (askOrders) {

                                    sortedAskOrders.put(assetId, new TreeSet<AskOrder>());

                                }
                                synchronized (bidOrders) {

                                    sortedBidOrders.put(assetId, new TreeSet<BidOrder>());

                                }
                                synchronized (senderAccount) {

                                    senderAccount.assetBalances.put(assetId, attachment.quantity);
                                    senderAccount.unconfirmedAssetBalances.put(assetId, attachment.quantity);

                                }

                            }
                            break;

                            case Transaction.SUBTYPE_COLORED_COINS_ASSET_TRANSFER:
                            {

                                Transaction.ColoredCoinsAssetTransferAttachment attachment = (Transaction.ColoredCoinsAssetTransferAttachment)transaction.attachment;

                                synchronized (senderAccount) {

                                    senderAccount.assetBalances.put(attachment.asset, senderAccount.assetBalances.get(attachment.asset) - attachment.quantity);
                                    senderAccount.unconfirmedAssetBalances.put(attachment.asset, senderAccount.unconfirmedAssetBalances.get(attachment.asset) - attachment.quantity);

                                }
                                synchronized (recipientAccount) {

                                    Integer assetBalance = recipientAccount.assetBalances.get(attachment.asset);
                                    if (assetBalance == null) {

                                        recipientAccount.assetBalances.put(attachment.asset, attachment.quantity);
                                        recipientAccount.unconfirmedAssetBalances.put(attachment.asset, attachment.quantity);

                                    } else {

                                        recipientAccount.assetBalances.put(attachment.asset, assetBalance + attachment.quantity);
                                        recipientAccount.unconfirmedAssetBalances.put(attachment.asset, recipientAccount.unconfirmedAssetBalances.get(attachment.asset) + attachment.quantity);

                                    }

                                }

                            }
                            break;

                            case Transaction.SUBTYPE_COLORED_COINS_ASK_ORDER_PLACEMENT:
                            {

                                Transaction.ColoredCoinsAskOrderPlacementAttachment attachment = (Transaction.ColoredCoinsAskOrderPlacementAttachment)transaction.attachment;

                                AskOrder order = new AskOrder(transaction.getId(), senderAccount, attachment.asset, attachment.quantity, attachment.price);
                                synchronized (senderAccount) {

                                    senderAccount.assetBalances.put(attachment.asset, senderAccount.assetBalances.get(attachment.asset) - attachment.quantity);
                                    senderAccount.unconfirmedAssetBalances.put(attachment.asset, senderAccount.unconfirmedAssetBalances.get(attachment.asset) - attachment.quantity);

                                }
                                synchronized (askOrders) {

                                    askOrders.put(order.id, order);
                                    sortedAskOrders.get(attachment.asset).add(order);

                                }

                                matchOrders(attachment.asset);

                            }
                            break;

                            case Transaction.SUBTYPE_COLORED_COINS_BID_ORDER_PLACEMENT:
                            {

                                Transaction.ColoredCoinsBidOrderPlacementAttachment attachment = (Transaction.ColoredCoinsBidOrderPlacementAttachment)transaction.attachment;

                                BidOrder order = new BidOrder(transaction.getId(), senderAccount, attachment.asset, attachment.quantity, attachment.price);
                                synchronized (senderAccount) {

                                    senderAccount.setBalance(senderAccount.balance - attachment.quantity * attachment.price);
                                    senderAccount.setUnconfirmedBalance(senderAccount.unconfirmedBalance - attachment.quantity * attachment.price);

                                }
                                synchronized (bidOrders) {

                                    bidOrders.put(order.id, order);
                                    sortedBidOrders.get(attachment.asset).add(order);

                                }

                                matchOrders(attachment.asset);

                            }
                            break;

                            case Transaction.SUBTYPE_COLORED_COINS_ASK_ORDER_CANCELLATION:
                            {

                                Transaction.ColoredCoinsAskOrderCancellationAttachment attachment = (Transaction.ColoredCoinsAskOrderCancellationAttachment)transaction.attachment;

                                AskOrder order;
                                synchronized (askOrders) {

                                    order = askOrders.remove(attachment.order);
                                    sortedAskOrders.get(order.asset).remove(order);

                                }
                                synchronized (senderAccount) {

                                    senderAccount.assetBalances.put(order.asset, senderAccount.assetBalances.get(order.asset) + order.quantity);
                                    senderAccount.unconfirmedAssetBalances.put(order.asset, senderAccount.unconfirmedAssetBalances.get(order.asset) + order.quantity);

                                }

                            }
                            break;

                            case Transaction.SUBTYPE_COLORED_COINS_BID_ORDER_CANCELLATION:
                            {

                                Transaction.ColoredCoinsBidOrderCancellationAttachment attachment = (Transaction.ColoredCoinsBidOrderCancellationAttachment)transaction.attachment;

                                BidOrder order;
                                synchronized (bidOrders) {

                                    order = bidOrders.remove(attachment.order);
                                    sortedBidOrders.get(order.asset).remove(order);

                                }
                                synchronized (senderAccount) {

                                    senderAccount.setBalance(senderAccount.balance + order.quantity * order.price);
                                    senderAccount.setUnconfirmedBalance(senderAccount.unconfirmedBalance + order.quantity * order.price);

                                }

                            }
                            break;

                        }

                    }
                    break;

                }

            }

		}
		
		static long getBaseTarget() throws Exception {
			
			if (lastBlock == GENESIS_BLOCK_ID) {
				
				return blocks.get(GENESIS_BLOCK_ID).baseTarget;
				
			}
			
			Block lastBlock = getLastBlock(), previousBlock = blocks.get(lastBlock.previousBlock);
			long curBaseTarget = previousBlock.baseTarget, newBaseTarget = BigInteger.valueOf(curBaseTarget).multiply(BigInteger.valueOf(lastBlock.timestamp - previousBlock.timestamp)).divide(BigInteger.valueOf(60)).longValue();
			if (newBaseTarget < 0 || newBaseTarget > maxBaseTarget) {
				
				newBaseTarget = maxBaseTarget;
				
			}
			
			if (newBaseTarget < curBaseTarget / 2) {
				
				newBaseTarget = curBaseTarget / 2;
				
			}
			if (newBaseTarget == 0) {
				
				newBaseTarget = 1;
				
			}
			
			long twofoldCurBaseTarget = curBaseTarget * 2;
			if (twofoldCurBaseTarget < 0) {
				
				twofoldCurBaseTarget = maxBaseTarget;
				
			}
			if (newBaseTarget > twofoldCurBaseTarget) {
				
				newBaseTarget = twofoldCurBaseTarget;
				
			}
			
			return newBaseTarget;
			
		}
		
		static Block getBlock(JSONObject blockData) {
			
			int version = ((Long)blockData.get("version")).intValue();
			int timestamp = ((Long)blockData.get("timestamp")).intValue();
			long previousBlock = (new BigInteger((String)blockData.get("previousBlock"))).longValue();
			int numberOfTransactions = ((Long)blockData.get("numberOfTransactions")).intValue();
			int totalAmount = ((Long)blockData.get("totalAmount")).intValue();
			int totalFee = ((Long)blockData.get("totalFee")).intValue();
			int payloadLength = ((Long)blockData.get("payloadLength")).intValue();
			byte[] payloadHash = convert((String)blockData.get("payloadHash"));
			byte[] generatorPublicKey = convert((String)blockData.get("generatorPublicKey"));
			byte[] generationSignature = convert((String)blockData.get("generationSignature"));
			byte[] blockSignature = convert((String)blockData.get("blockSignature"));
			
			if (version == 1) {
				
				return new Block(version, timestamp, previousBlock, numberOfTransactions, totalAmount, totalFee, payloadLength, payloadHash, generatorPublicKey, generationSignature, blockSignature);
				
			} else {
				
				byte[] previousBlockHash = convert((String)blockData.get("previousBlockHash"));
				
				return new Block(version, timestamp, previousBlock, numberOfTransactions, totalAmount, totalFee, payloadLength, payloadHash, generatorPublicKey, generationSignature, blockSignature, previousBlockHash);
				
			}

        }

        //TODO: cache this byte[] ?
		byte[] getBytes() {
			
			ByteBuffer buffer = ByteBuffer.allocate(4 + 4 + 8 + 4 + 4 + 4 + 4 + 32 + 32 + (32 + 32) + 64);
			buffer.order(ByteOrder.LITTLE_ENDIAN);
			buffer.putInt(version);
			buffer.putInt(timestamp);
			buffer.putLong(previousBlock);
			buffer.putInt(numberOfTransactions);
			buffer.putInt(totalAmount);
			buffer.putInt(totalFee);
			buffer.putInt(payloadLength);
			buffer.put(payloadHash);
			buffer.put(generatorPublicKey);
			//logMessage(generationSignature.length+":"+previousBlockHash);
			buffer.put(generationSignature);
			if (version > 1) {
				
				//logMessage("@ "+getLastBlock().height);
				buffer.put(previousBlockHash);
				
			}
			buffer.put(blockSignature);
			
			return buffer.array();
			
		}
		
        //TODO: again, cache the id
		long getId() throws Exception {
			
			byte[] hash = MessageDigest.getInstance("SHA-256").digest(getBytes());
			BigInteger bigInteger = new BigInteger(1, new byte[] {hash[7], hash[6], hash[5], hash[4], hash[3], hash[2], hash[1], hash[0]});
			return bigInteger.longValue();
			
		}
		
		JSONObject getJSONObject(Map<Long, Transaction> transactions) {
			
			JSONObject block = new JSONObject();
			
			block.put("version", version);
			block.put("timestamp", timestamp);
			block.put("previousBlock", convert(previousBlock));
			block.put("numberOfTransactions", numberOfTransactions);
			block.put("totalAmount", totalAmount);
			block.put("totalFee", totalFee);
			block.put("payloadLength", payloadLength);
			block.put("payloadHash", convert(payloadHash));
			block.put("generatorPublicKey", convert(generatorPublicKey));
			block.put("generationSignature", convert(generationSignature));
			if (version > 1) {
				
				block.put("previousBlockHash", convert(previousBlockHash));
				
			}
			block.put("blockSignature", convert(blockSignature));
			
			JSONArray transactionsData = new JSONArray();
			for (int i = 0; i < numberOfTransactions; i++) {
				
				transactionsData.add(transactions.get(this.transactions[i]).getJSONObject());
				
			}
			block.put("transactions", transactionsData);
			
			return block;
			
		}
		
		static Block getLastBlock() {
			
			return blocks.get(lastBlock);
			
		}
		
		static void loadBlocks(String fileName) throws Exception {

            try (FileInputStream fileInputStream = new FileInputStream(fileName);
                 ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream)
            ) {
                blockCounter.set(objectInputStream.readInt());
                blocks.clear();
                blocks.putAll((HashMap<Long, Block>) objectInputStream.readObject());
                lastBlock = objectInputStream.readLong();
            }

        }
		
		static boolean popLastBlock() {
			
			if (lastBlock == GENESIS_BLOCK_ID) {
				
				return false;
				
			}
			
			try {
				
				JSONObject response = new JSONObject();
				response.put("response", "processNewData");
				
				JSONArray addedUnconfirmedTransactions = new JSONArray();

                Block block;

				synchronized (blocksAndTransactionsLock) {
					
					block = Block.getLastBlock();
					
					Account generatorAccount = accounts.get(Account.getId(block.generatorPublicKey));
					synchronized (generatorAccount) {
						
						generatorAccount.setBalance(generatorAccount.balance - block.totalFee * 100L);
						generatorAccount.setUnconfirmedBalance(generatorAccount.unconfirmedBalance - block.totalFee * 100L);
						
					}

                    for (int i = 0; i < block.numberOfTransactions; i++) {

                        Transaction transaction = Nxt.transactions.remove(block.transactions[i]);
                        unconfirmedTransactions.put(block.transactions[i], transaction);

                        Account senderAccount = accounts.get(Account.getId(transaction.senderPublicKey));
                        synchronized (senderAccount) {

                            senderAccount.setBalance(senderAccount.balance + (transaction.amount + transaction.fee) * 100L);

                        }

                        Account recipientAccount = accounts.get(transaction.recipient);
                        synchronized (recipientAccount) {

                            recipientAccount.setBalance(recipientAccount.balance - transaction.amount * 100L);
                            recipientAccount.setUnconfirmedBalance(recipientAccount.unconfirmedBalance - transaction.amount * 100L);

                        }

                        JSONObject addedUnconfirmedTransaction = new JSONObject();
                        addedUnconfirmedTransaction.put("index", transaction.index);
                        addedUnconfirmedTransaction.put("timestamp", transaction.timestamp);
                        addedUnconfirmedTransaction.put("deadline", transaction.deadline);
                        addedUnconfirmedTransaction.put("recipient", convert(transaction.recipient));
                        addedUnconfirmedTransaction.put("amount", transaction.amount);
                        addedUnconfirmedTransaction.put("fee", transaction.fee);
                        addedUnconfirmedTransaction.put("sender", convert(Account.getId(transaction.senderPublicKey)));
                        addedUnconfirmedTransaction.put("id", convert(transaction.getId()));
                        addedUnconfirmedTransactions.add(addedUnconfirmedTransaction);

                    }

					lastBlock = block.previousBlock;
					
				}

                JSONArray addedOrphanedBlocks = new JSONArray();
                JSONObject addedOrphanedBlock = new JSONObject();
                addedOrphanedBlock.put("index", block.index);
                addedOrphanedBlock.put("timestamp", block.timestamp);
                addedOrphanedBlock.put("numberOfTransactions", block.numberOfTransactions);
                addedOrphanedBlock.put("totalAmount", block.totalAmount);
                addedOrphanedBlock.put("totalFee", block.totalFee);
                addedOrphanedBlock.put("payloadLength", block.payloadLength);
                addedOrphanedBlock.put("generator", convert(Account.getId(block.generatorPublicKey)));
                addedOrphanedBlock.put("height", block.height);
                addedOrphanedBlock.put("version", block.version);
                addedOrphanedBlock.put("block", convert(block.getId()));
                addedOrphanedBlock.put("baseTarget", BigInteger.valueOf(block.baseTarget).multiply(BigInteger.valueOf(100000)).divide(BigInteger.valueOf(initialBaseTarget)));
                addedOrphanedBlocks.add(addedOrphanedBlock);
                response.put("addedOrphanedBlocks", addedOrphanedBlocks);

                if (addedUnconfirmedTransactions.size() > 0) {
					
					response.put("addedUnconfirmedTransactions", addedUnconfirmedTransactions);
					
				}
				
				for (User user : users.values()) {
					
					user.send(response);
					
				}
				
			} catch (Exception e) {
				
				logMessage("19: " + e.toString());
				
				return false;
				
			}
			
			return true;
			
		}
		
		static boolean pushBlock(ByteBuffer buffer, boolean savingFlag) throws Exception {
			
			buffer.flip();
			
			int version = buffer.getInt();
			if (version != (getLastBlock().height < TRANSPARENT_FORGING_BLOCK ? 1 : 2)) {
				
				return false;
				
			}
			
			int blockTimestamp = buffer.getInt();
			long previousBlock = buffer.getLong();
			int numberOfTransactions = buffer.getInt();
			int totalAmount = buffer.getInt();
			int totalFee = buffer.getInt();
			int payloadLength = buffer.getInt();
			byte[] payloadHash = new byte[32];
			buffer.get(payloadHash);
			byte[] generatorPublicKey = new byte[32];
			buffer.get(generatorPublicKey);
			byte[] generationSignature;
			byte[] previousBlockHash;
			if (version == 1) {
				
				generationSignature = new byte[64];
				buffer.get(generationSignature);
				previousBlockHash = null;
				
			} else {
				
				generationSignature = new byte[32];
				buffer.get(generationSignature);
				previousBlockHash = new byte[32];
				buffer.get(previousBlockHash);
				
				if (!Arrays.equals(MessageDigest.getInstance("SHA-256").digest(Block.getLastBlock().getBytes()), previousBlockHash)) {
					
					return false;
					
				}
				
			}
			byte[] blockSignature = new byte[64];
			buffer.get(blockSignature);
			
			int curTime = getEpochTime(System.currentTimeMillis());
			if (blockTimestamp > curTime + 15 || blockTimestamp <= Block.getLastBlock().timestamp) {
				
				return false;
				
			}
			
			if (payloadLength > MAX_PAYLOAD_LENGTH || BLOCK_HEADER_LENGTH + payloadLength != buffer.capacity()) {
				
				return false;
				
			}
			
			Block block;
			if (version == 1) {
				
				block = new Block(version, blockTimestamp, previousBlock, numberOfTransactions, totalAmount, totalFee, payloadLength, payloadHash, generatorPublicKey, generationSignature, blockSignature);
				
			} else {
				
				block = new Block(version, blockTimestamp, previousBlock, numberOfTransactions, totalAmount, totalFee, payloadLength, payloadHash, generatorPublicKey, generationSignature, blockSignature, previousBlockHash);
				
			}

            block.index = blockCounter.incrementAndGet();

			try {
				
				if (block.previousBlock != lastBlock || blocks.get(block.getId()) != null || !block.verifyGenerationSignature() || !block.verifyBlockSignature()) {
					
					return false;
					
				}
				
				HashMap<Long, Transaction> blockTransactions = new HashMap<>();
				HashSet<String> blockAliases = new HashSet<>();
				block.transactions = new long[block.numberOfTransactions];
				for (int i = 0; i < block.numberOfTransactions; i++) {
					
					Transaction transaction = Transaction.getTransaction(buffer);
                    transaction.index = transactionCounter.incrementAndGet();

                    if (blockTransactions.put(block.transactions[i] = transaction.getId(), transaction) != null) {
						
						return false;
						
					}
					
					switch (transaction.type) {
					
					case Transaction.TYPE_MESSAGING:
						{
							
							switch (transaction.subtype) {
							
							case Transaction.SUBTYPE_MESSAGING_ALIAS_ASSIGNMENT:
								{
									
									if (!blockAliases.add(((Transaction.MessagingAliasAssignmentAttachment)transaction.attachment).alias.toLowerCase())) {
										
										return false;
										
									}
									
								}
								break;
								
							}
							
						}
						break;
						
					}
					
				}
				Arrays.sort(block.transactions);
				
				HashMap<Long, Long> accumulatedAmounts = new HashMap<>();
				HashMap<Long, HashMap<Long, Long>> accumulatedAssetQuantities = new HashMap<>();
				int calculatedTotalAmount = 0, calculatedTotalFee = 0;
				int i;
				for (i = 0; i < block.numberOfTransactions; i++) {
					
					Transaction transaction = blockTransactions.get(block.transactions[i]);
					
					if (transaction.timestamp > curTime + 15 || transaction.deadline < 1 || (transaction.timestamp + transaction.deadline * 60 < blockTimestamp && getLastBlock().height > 303) || transaction.fee <= 0 || !transaction.validateAttachment() || Nxt.transactions.get(block.transactions[i]) != null || (transaction.referencedTransaction != 0 && Nxt.transactions.get(transaction.referencedTransaction) == null && blockTransactions.get(transaction.referencedTransaction) == null) || (unconfirmedTransactions.get(block.transactions[i]) == null && !transaction.verify())) {
						
						break;
						
					}
					
					long sender = Account.getId(transaction.senderPublicKey);
					Long accumulatedAmount = accumulatedAmounts.get(sender);
					if (accumulatedAmount == null) {
						
						accumulatedAmount = 0L;
						
					}
					accumulatedAmounts.put(sender, accumulatedAmount + (transaction.amount + transaction.fee) * 100L);
					if (transaction.type == Transaction.TYPE_PAYMENT) {
						
						if (transaction.subtype == Transaction.SUBTYPE_PAYMENT_ORDINARY_PAYMENT) {
							
							calculatedTotalAmount += transaction.amount;
							
						} else {
							
							break;
							
						}
						
					} else if (transaction.type == Transaction.TYPE_MESSAGING) {
						
						if (transaction.subtype != Transaction.SUBTYPE_MESSAGING_ALIAS_ASSIGNMENT) {
							
							break;
							
						}
						
					} else if (transaction.type == Transaction.TYPE_COLORED_COINS) {
						
						if (transaction.subtype == Transaction.SUBTYPE_COLORED_COINS_ASSET_TRANSFER) {
							
							Transaction.ColoredCoinsAssetTransferAttachment attachment = (Transaction.ColoredCoinsAssetTransferAttachment)transaction.attachment;
							HashMap<Long, Long> accountAccumulatedAssetQuantities = accumulatedAssetQuantities.get(sender);
							if (accountAccumulatedAssetQuantities == null) {
								
								accountAccumulatedAssetQuantities = new HashMap<>();
								accumulatedAssetQuantities.put(sender, accountAccumulatedAssetQuantities);
								
							}
							Long assetAccumulatedAssetQuantities = accountAccumulatedAssetQuantities.get(attachment.asset);
							if (assetAccumulatedAssetQuantities == null) {
								
								assetAccumulatedAssetQuantities = new Long(0);
								
							}
							accountAccumulatedAssetQuantities.put(attachment.asset, assetAccumulatedAssetQuantities + attachment.quantity);
							
						} else if (transaction.subtype == Transaction.SUBTYPE_COLORED_COINS_ASK_ORDER_PLACEMENT) {
							
							Transaction.ColoredCoinsAskOrderPlacementAttachment attachment = (Transaction.ColoredCoinsAskOrderPlacementAttachment)transaction.attachment;
							HashMap<Long, Long> accountAccumulatedAssetQuantities = accumulatedAssetQuantities.get(sender);
							if (accountAccumulatedAssetQuantities == null) {
								
								accountAccumulatedAssetQuantities = new HashMap<>();
								accumulatedAssetQuantities.put(sender, accountAccumulatedAssetQuantities);
								
							}
							Long assetAccumulatedAssetQuantities = accountAccumulatedAssetQuantities.get(attachment.asset);
							if (assetAccumulatedAssetQuantities == null) {
								
								assetAccumulatedAssetQuantities = new Long(0);
								
							}
							accountAccumulatedAssetQuantities.put(attachment.asset, assetAccumulatedAssetQuantities + attachment.quantity);
							
						} else if (transaction.subtype == Transaction.SUBTYPE_COLORED_COINS_BID_ORDER_PLACEMENT) {
							
							Transaction.ColoredCoinsBidOrderPlacementAttachment attachment = (Transaction.ColoredCoinsBidOrderPlacementAttachment)transaction.attachment;
							accumulatedAmounts.put(sender, accumulatedAmount + attachment.quantity * attachment.price);
							
						} else if (transaction.subtype != Transaction.SUBTYPE_COLORED_COINS_ASSET_ISSUANCE && transaction.subtype != Transaction.SUBTYPE_COLORED_COINS_ASK_ORDER_CANCELLATION && transaction.subtype != Transaction.SUBTYPE_COLORED_COINS_BID_ORDER_CANCELLATION) {
							
							break;
							
						}
						
					} else {
						
						break;
						
					}
					calculatedTotalFee += transaction.fee;
					
				}
				
				if (i != block.numberOfTransactions || calculatedTotalAmount != block.totalAmount || calculatedTotalFee != block.totalFee) {
					
					return false;
					
				}
				
				MessageDigest digest = MessageDigest.getInstance("SHA-256");
				for (i = 0; i < block.numberOfTransactions; i++) {
					
					digest.update(blockTransactions.get(block.transactions[i]).getBytes());
					
				}
				if (!Arrays.equals(digest.digest(), block.payloadHash)) {
					
					return false;
					
				}

                JSONArray addedConfirmedTransactions;
                JSONArray removedUnconfirmedTransactions;

                synchronized (blocksAndTransactionsLock) {
					
					for (Map.Entry<Long, Long> accumulatedAmountEntry : accumulatedAmounts.entrySet()) {
						
						Account senderAccount = accounts.get(accumulatedAmountEntry.getKey());
						if (senderAccount.balance < accumulatedAmountEntry.getValue()) {
							
							return false;
							
						}
						
					}
					
					for (Map.Entry<Long, HashMap<Long, Long>> accumulatedAssetQuantitiesEntry : accumulatedAssetQuantities.entrySet()) {
						
						Account senderAccount = accounts.get(accumulatedAssetQuantitiesEntry.getKey());
						for (Map.Entry<Long, Long> accountAccumulatedAssetQuantitiesEntry : accumulatedAssetQuantitiesEntry.getValue().entrySet()) {
							
							long asset = accountAccumulatedAssetQuantitiesEntry.getKey();
							long quantity = accountAccumulatedAssetQuantitiesEntry.getValue();
							if (senderAccount.assetBalances.get(asset) < quantity) {
								
								return false;
								
							}
							
						}
						
					}
					
					if (block.previousBlock != lastBlock) {
						
						return false;
						
					}

                    for (Map.Entry<Long, Transaction> transactionEntry : blockTransactions.entrySet()) {

                        Transaction transaction = transactionEntry.getValue();
                        transaction.height = block.height;
                        Nxt.transactions.put(transactionEntry.getKey(), transaction);

                    }

					block.analyze();
					
					addedConfirmedTransactions = new JSONArray();
					removedUnconfirmedTransactions = new JSONArray();
					
					for (Map.Entry<Long, Transaction> transactionEntry : blockTransactions.entrySet()) {
						
						Transaction transaction = transactionEntry.getValue();
						
						JSONObject addedConfirmedTransaction = new JSONObject();
						addedConfirmedTransaction.put("index", transaction.index);
						addedConfirmedTransaction.put("blockTimestamp", block.timestamp);
						addedConfirmedTransaction.put("transactionTimestamp", transaction.timestamp);
						addedConfirmedTransaction.put("sender", convert(Account.getId(transaction.senderPublicKey)));
						addedConfirmedTransaction.put("recipient", convert(transaction.recipient));
						addedConfirmedTransaction.put("amount", transaction.amount);
						addedConfirmedTransaction.put("fee", transaction.fee);
						addedConfirmedTransaction.put("id", convert(transaction.getId()));
						addedConfirmedTransactions.add(addedConfirmedTransaction);
						
						Transaction removedTransaction = unconfirmedTransactions.remove(transactionEntry.getKey());
						if (removedTransaction != null) {
							
							JSONObject removedUnconfirmedTransaction = new JSONObject();
							removedUnconfirmedTransaction.put("index", removedTransaction.index);
							removedUnconfirmedTransactions.add(removedUnconfirmedTransaction);
							
							Account senderAccount = accounts.get(Account.getId(removedTransaction.senderPublicKey));
							synchronized (senderAccount) {
								
								senderAccount.setUnconfirmedBalance(senderAccount.unconfirmedBalance + (removedTransaction.amount + removedTransaction.fee) * 100L);
								
							}
							
						}
						
						// TODO: Remove from double-spending transactions
						
					}
					
					long blockId = block.getId();
					for (i = 0; i < block.transactions.length; i++) {
						
						Nxt.transactions.get(block.transactions[i]).block = blockId;
						
					}
					
					if (savingFlag) {

						Transaction.saveTransactions("transactions.nxt");
						Block.saveBlocks("blocks.nxt", false);
						
					}

                }

                if (block.timestamp >= curTime - 15) {

                    JSONObject request = block.getJSONObject(Nxt.transactions);
                    request.put("requestType", "processBlock");

                    Peer.sendToAllPeers(request);

                }

                JSONArray addedRecentBlocks = new JSONArray();
                JSONObject addedRecentBlock = new JSONObject();
                addedRecentBlock.put("index", block.index);
                addedRecentBlock.put("timestamp", block.timestamp);
                addedRecentBlock.put("numberOfTransactions", block.numberOfTransactions);
                addedRecentBlock.put("totalAmount", block.totalAmount);
                addedRecentBlock.put("totalFee", block.totalFee);
                addedRecentBlock.put("payloadLength", block.payloadLength);
                addedRecentBlock.put("generator", convert(Account.getId(block.generatorPublicKey)));
                addedRecentBlock.put("height", Block.getLastBlock().height);
                addedRecentBlock.put("version", block.version);
                addedRecentBlock.put("block", convert(block.getId()));
                addedRecentBlock.put("baseTarget", BigInteger.valueOf(block.baseTarget).multiply(BigInteger.valueOf(100000)).divide(BigInteger.valueOf(initialBaseTarget)));
                addedRecentBlocks.add(addedRecentBlock);

                JSONObject response = new JSONObject();
                response.put("response", "processNewData");
                response.put("addedConfirmedTransactions", addedConfirmedTransactions);
                if (removedUnconfirmedTransactions.size() > 0) {

                    response.put("removedUnconfirmedTransactions", removedUnconfirmedTransactions);

                }
                response.put("addedRecentBlocks", addedRecentBlocks);

                for (User user : users.values()) {

                    user.send(response);

                }

				return true;
				
			} catch (Exception e) {
				
				logMessage("11: " + e.toString());
				
				return false;
				
			}
			
		}
		
		static void saveBlocks(String fileName, boolean flag) throws Exception {

            try (FileOutputStream fileOutputStream = new FileOutputStream(fileName);
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream)
            ) {
                objectOutputStream.writeInt(blockCounter.get());
                objectOutputStream.writeObject(new HashMap<>(blocks));
                objectOutputStream.writeLong(lastBlock);
            }

				/*if (flag) {
					
					ByteBuffer buffer = ByteBuffer.allocate(BLOCK_HEADER_LENGTH + MAX_PAYLOAD_LENGTH);
					buffer.order(ByteOrder.LITTLE_ENDIAN);
					
					long curBlockId = GENESIS_BLOCK_ID;
					long prevBlockPtr = -1, curBlockPtr = 0;
					MessageDigest digest = MessageDigest.getInstance("SHA-256");
					do {
						
						Block block = blocks.get(curBlockId);
						buffer.clear();
						buffer.putLong(prevBlockPtr);
						buffer.put(block.getBytes());
						for (int i = 0; i < block.numberOfTransactions; i++) {
							
							buffer.put(Nxt.transactions.get(block.transactions[i]).getBytes());
							
						}
						buffer.flip();
						byte[] rawBytes = new byte[buffer.limit()];
						buffer.get(rawBytes);
						
						MappedByteBuffer window = blockchainChannel.map(FileChannel.MapMode.READ_WRITE, curBlockPtr, 32 + rawBytes.length);
						window.put(digest.digest(rawBytes));
						window.put(rawBytes);
						
						prevBlockPtr = curBlockPtr;
						curBlockPtr += 32 + rawBytes.length;
						curBlockId = block.nextBlock;
						
					} while (curBlockId != 0);
					
				}*/

		}
		
		boolean verifyBlockSignature() throws Exception {
			
			Account account = accounts.get(Account.getId(generatorPublicKey));
			if (account == null) {
				
				return false;
				
			} else if (account.publicKey == null) {
				
				account.publicKey = generatorPublicKey;
				
			} else if (!Arrays.equals(generatorPublicKey, account.publicKey)) {
				
				return false;
				
			}
			
			byte[] data = getBytes();
			byte[] data2 = new byte[data.length - 64];
			System.arraycopy(data, 0, data2, 0, data2.length);
			if (!Crypto.verify(blockSignature, data2, generatorPublicKey)) {
				
				return false;
				
			}
			
			return true;
			
		}
		
		boolean verifyGenerationSignature() {
			
			try {
				
				Block previousBlock = blocks.get(this.previousBlock);
				if (previousBlock == null) {
					
					return false;
					
				}
				
				if (version == 1 && !Crypto.verify(generationSignature, previousBlock.generationSignature, generatorPublicKey)) {
					
					return false;
					
				}
				
				Account account = accounts.get(Account.getId(generatorPublicKey));
				if (account == null || account.getEffectiveBalance() == 0) {
					
					return false;
					
				}
				
				int elapsedTime = timestamp - previousBlock.timestamp;
				BigInteger target = BigInteger.valueOf(Block.getBaseTarget()).multiply(BigInteger.valueOf(account.getEffectiveBalance())).multiply(BigInteger.valueOf(elapsedTime));
				
				MessageDigest digest = MessageDigest.getInstance("SHA-256");
				byte[] generationSignatureHash;
				if (version == 1) {
					
					generationSignatureHash = digest.digest(generationSignature);
					
				} else {
					
					digest.update(previousBlock.generationSignature);
					generationSignatureHash = digest.digest(generatorPublicKey);
					if (!Arrays.equals(generationSignature, generationSignatureHash)) {
						
						return false;
						
					}
					
				}
				
				BigInteger hit = new BigInteger(1, new byte[] {generationSignatureHash[7], generationSignatureHash[6], generationSignatureHash[5], generationSignatureHash[4], generationSignatureHash[3], generationSignatureHash[2], generationSignatureHash[1], generationSignatureHash[0]});
				if (hit.compareTo(target) >= 0) {
					
					return false;
					
				}
				
				return true;
				
			} catch (Exception e) {
				
				return false;
				
			}
			
		}
		
	}
	
	static class Crypto {
		
		static byte[] getPublicKey(String secretPhrase) {
			
			try {
				
				byte[] publicKey = new byte[32];
				Curve25519.keygen(publicKey, null, MessageDigest.getInstance("SHA-256").digest(secretPhrase.getBytes("UTF-8")));
				
				return publicKey;
				
			} catch (Exception e) {
				
				return null;
				
			}
			
		}
		
		static byte[] sign(byte[] message, String secretPhrase) {
			
			try {
				
				byte[] P = new byte[32];
				byte[] s = new byte[32];
				MessageDigest digest = MessageDigest.getInstance("SHA-256");
				Curve25519.keygen(P, s, digest.digest(secretPhrase.getBytes("UTF-8")));
				
				byte[] m = digest.digest(message);
				
				digest.update(m);
				byte[] x = digest.digest(s);
				
				byte[] Y = new byte[32];
				Curve25519.keygen(Y, null, x);
				
				digest.update(m);
				byte[] h = digest.digest(Y);
				
				byte[] v = new byte[32];
				Curve25519.sign(v, h, x, s);
				
				byte[] signature = new byte[64];
				System.arraycopy(v, 0, signature, 0, 32);
				System.arraycopy(h, 0, signature, 32, 32);
				
				return signature;
				
			} catch (Exception e) {
				
				return null;
				
			}
			
		}
		
		static boolean verify(byte[] signature, byte[] message, byte[] publicKey) {
			
			try {
				
				byte[] Y = new byte[32];
				byte[] v = new byte[32];
				System.arraycopy(signature, 0, v, 0, 32);
				byte[] h = new byte[32];
				System.arraycopy(signature, 32, h, 0, 32);
				Curve25519.verify(Y, v, h, publicKey);
				
				MessageDigest digest = MessageDigest.getInstance("SHA-256");
				byte[] m = digest.digest(message);
				digest.update(m);
				byte[] h2 = digest.digest(Y);
				
				return Arrays.equals(h, h2);
				
			} catch (Exception e) {
				
				return false;
				
			}
			
		}
		
	}
	
	/* Ported from C to Java by Dmitry Skiba [sahn0], 23/02/08.
	 * Original: http://cds.xs4all.nl:8081/ecdh/
	 */
	/* Generic 64-bit integer implementation of Curve25519 ECDH
	 * Written by Matthijs van Duin, 200608242056
	 * Public domain.
	 *
	 * Based on work by Daniel J Bernstein, http://cr.yp.to/ecdh.html
	 */
	static class Curve25519 {

		/* key size */
		public static final int KEY_SIZE = 32;
		
		/* 0 */
		public static final byte[] ZERO = {
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
		};
		
		/* the prime 2^255-19 */
		public static final byte[] PRIME = {
			(byte)237, (byte)255, (byte)255, (byte)255,
			(byte)255, (byte)255, (byte)255, (byte)255,
			(byte)255, (byte)255, (byte)255, (byte)255,
			(byte)255, (byte)255, (byte)255, (byte)255,
			(byte)255, (byte)255, (byte)255, (byte)255,
			(byte)255, (byte)255, (byte)255, (byte)255,
		    (byte)255, (byte)255, (byte)255, (byte)255,
		    (byte)255, (byte)255, (byte)255, (byte)127
		};
		
		/* group order (a prime near 2^252+2^124) */
		public static final byte[] ORDER = {
			(byte)237, (byte)211, (byte)245, (byte)92,
			(byte)26,  (byte)99,  (byte)18,  (byte)88,
			(byte)214, (byte)156, (byte)247, (byte)162,
			(byte)222, (byte)249, (byte)222, (byte)20,
			(byte)0,   (byte)0,   (byte)0,   (byte)0, 
			(byte)0,   (byte)0,   (byte)0,   (byte)0,
			(byte)0,   (byte)0,   (byte)0,   (byte)0, 
			(byte)0,   (byte)0,   (byte)0,   (byte)16
		};
		
		/********* KEY AGREEMENT *********/
		
		/* Private key clamping
		 *   k [out] your private key for key agreement
		 *   k  [in]  32 random bytes
		 */
		public static final void clamp(byte[] k) {
			k[31] &= 0x7F;
			k[31] |= 0x40;
			k[ 0] &= 0xF8;
		}
		
		/* Key-pair generation
		 *   P  [out] your public key
		 *   s  [out] your private key for signing
		 *   k  [out] your private key for key agreement
		 *   k  [in]  32 random bytes
		 * s may be NULL if you don't care
		 *
		 * WARNING: if s is not NULL, this function has data-dependent timing */
		public static final void keygen(byte[] P, byte[] s, byte[] k) {
			clamp(k);
			core(P, s, k, null);
		}

		/* Key agreement
		 *   Z  [out] shared secret (needs hashing before use)
		 *   k  [in]  your private key for key agreement
		 *   P  [in]  peer's public key
		 */
		public static final void curve(byte[] Z, byte[] k, byte[] P) {
			core(Z, null, k, P);
		}
		
		/********* DIGITAL SIGNATURES *********/

		/* deterministic EC-KCDSA
		 *
		 *    s is the private key for signing
		 *    P is the corresponding public key
		 *    Z is the context data (signer public key or certificate, etc)
		 *
		 * signing:
		 *
		 *    m = hash(Z, message)
		 *    x = hash(m, s)
		 *    keygen25519(Y, NULL, x);
		 *    r = hash(Y);
		 *    h = m XOR r
		 *    sign25519(v, h, x, s);
		 *
		 *    output (v,r) as the signature
		 *
		 * verification:
		 *
		 *    m = hash(Z, message);
		 *    h = m XOR r
		 *    verify25519(Y, v, h, P)
		 *
		 *    confirm  r == hash(Y)
		 *
		 * It would seem to me that it would be simpler to have the signer directly do 
		 * h = hash(m, Y) and send that to the recipient instead of r, who can verify 
		 * the signature by checking h == hash(m, Y).  If there are any problems with 
		 * such a scheme, please let me know.
		 *
		 * Also, EC-KCDSA (like most DS algorithms) picks x random, which is a waste of 
		 * perfectly good entropy, but does allow Y to be calculated in advance of (or 
		 * parallel to) hashing the message.
		 */
		
		/* Signature generation primitive, calculates (x-h)s mod q
		 *   v  [out] signature value
		 *   h  [in]  signature hash (of message, signature pub key, and context data)
		 *   x  [in]  signature private key
		 *   s  [in]  private key for signing
		 * returns true on success, false on failure (use different x or h)
		 */
		public static final boolean sign(byte[] v, byte[] h, byte[] x, byte[] s) {
			/* v = (x - h) s  mod q  */
			byte[] tmp1=new byte[65];
			byte[] tmp2=new byte[33];
			int w;
			int i;
			for (i = 0; i < 32; i++)
				v[i] = 0;
			i = mula_small(v, x, 0, h, 32, -1);
			mula_small(v, v, 0, ORDER, 32, (15-v[31])/16);
			mula32(tmp1, v, s, 32, 1);
			divmod(tmp2, tmp1, 64, ORDER, 32);
			for (w = 0, i = 0; i < 32; i++)
				w |= v[i] = tmp1[i];
			return w != 0;
		}
		
		/* Signature verification primitive, calculates Y = vP + hG
		 *   Y  [out] signature public key
		 *   v  [in]  signature value
		 *   h  [in]  signature hash
		 *   P  [in]  public key
		 */
		public static final void verify(byte[] Y, byte[] v, byte[] h, byte[] P) {
			/* Y = v abs(P) + h G  */
			byte[] d=new byte[32];
		    long10[] 
		        p=new long10[]{new long10(),new long10()},
		    	s=new long10[]{new long10(),new long10()},
			    yx=new long10[]{new long10(),new long10(),new long10()},
			    yz=new long10[]{new long10(),new long10(),new long10()},
			    t1=new long10[]{new long10(),new long10(),new long10()},
			    t2=new long10[]{new long10(),new long10(),new long10()};
			
			int vi = 0, hi = 0, di = 0, nvh=0, i, j, k;

			/* set p[0] to G and p[1] to P  */

			set(p[0], 9);
			unpack(p[1], P);

			/* set s[0] to P+G and s[1] to P-G  */

			/* s[0] = (Py^2 + Gy^2 - 2 Py Gy)/(Px - Gx)^2 - Px - Gx - 486662  */
			/* s[1] = (Py^2 + Gy^2 + 2 Py Gy)/(Px - Gx)^2 - Px - Gx - 486662  */

			x_to_y2(t1[0], t2[0], p[1]);	/* t2[0] = Py^2  */
			sqrt(t1[0], t2[0]);	/* t1[0] = Py or -Py  */
			j = is_negative(t1[0]);		/*      ... check which  */
			t2[0]._0 += 39420360;		/* t2[0] = Py^2 + Gy^2  */
			mul(t2[1], BASE_2Y, t1[0]);/* t2[1] = 2 Py Gy or -2 Py Gy  */
			sub(t1[j], t2[0], t2[1]);	/* t1[0] = Py^2 + Gy^2 - 2 Py Gy  */
			add(t1[1-j], t2[0], t2[1]);/* t1[1] = Py^2 + Gy^2 + 2 Py Gy  */
			cpy(t2[0], p[1]);		/* t2[0] = Px  */
			t2[0]._0 -= 9;			/* t2[0] = Px - Gx  */
			sqr(t2[1], t2[0]);		/* t2[1] = (Px - Gx)^2  */
			recip(t2[0], t2[1], 0);	/* t2[0] = 1/(Px - Gx)^2  */
			mul(s[0], t1[0], t2[0]);	/* s[0] = t1[0]/(Px - Gx)^2  */
			sub(s[0], s[0], p[1]);	/* s[0] = t1[0]/(Px - Gx)^2 - Px  */
			s[0]._0 -= 9 + 486662;		/* s[0] = X(P+G)  */
			mul(s[1], t1[1], t2[0]);	/* s[1] = t1[1]/(Px - Gx)^2  */
			sub(s[1], s[1], p[1]);	/* s[1] = t1[1]/(Px - Gx)^2 - Px  */
			s[1]._0 -= 9 + 486662;		/* s[1] = X(P-G)  */
			mul_small(s[0], s[0], 1);	/* reduce s[0] */
			mul_small(s[1], s[1], 1);	/* reduce s[1] */


			/* prepare the chain  */
			for (i = 0; i < 32; i++) {
				vi = (vi >> 8) ^ (v[i] & 0xFF) ^ ((v[i] & 0xFF) << 1);
				hi = (hi >> 8) ^ (h[i] & 0xFF) ^ ((h[i] & 0xFF) << 1);
				nvh = ~(vi ^ hi);
				di = (nvh & (di & 0x80) >> 7) ^ vi;
				di ^= nvh & (di & 0x01) << 1;
				di ^= nvh & (di & 0x02) << 1;
				di ^= nvh & (di & 0x04) << 1;
				di ^= nvh & (di & 0x08) << 1;
				di ^= nvh & (di & 0x10) << 1;
				di ^= nvh & (di & 0x20) << 1;
				di ^= nvh & (di & 0x40) << 1;
				d[i] = (byte)di;
			}

			di = ((nvh & (di & 0x80) << 1) ^ vi) >> 8;

			/* initialize state */
			set(yx[0], 1);
			cpy(yx[1], p[di]);
			cpy(yx[2], s[0]);
			set(yz[0], 0);
			set(yz[1], 1);
			set(yz[2], 1);

			/* y[0] is (even)P + (even)G
			 * y[1] is (even)P + (odd)G  if current d-bit is 0
			 * y[1] is (odd)P + (even)G  if current d-bit is 1
			 * y[2] is (odd)P + (odd)G
			 */

			vi = 0;
			hi = 0;

			/* and go for it! */
			for (i = 32; i--!=0; ) {
				vi = (vi << 8) | (v[i] & 0xFF);
				hi = (hi << 8) | (h[i] & 0xFF);
				di = (di << 8) | (d[i] & 0xFF);

				for (j = 8; j--!=0; ) {
					mont_prep(t1[0], t2[0], yx[0], yz[0]);
					mont_prep(t1[1], t2[1], yx[1], yz[1]);
					mont_prep(t1[2], t2[2], yx[2], yz[2]);

					k = ((vi ^ vi >> 1) >> j & 1)
					  + ((hi ^ hi >> 1) >> j & 1);
					mont_dbl(yx[2], yz[2], t1[k], t2[k], yx[0], yz[0]);

					k = (di >> j & 2) ^ ((di >> j & 1) << 1);
					mont_add(t1[1], t2[1], t1[k], t2[k], yx[1], yz[1],
							p[di >> j & 1]);

					mont_add(t1[2], t2[2], t1[0], t2[0], yx[2], yz[2],
							s[((vi ^ hi) >> j & 2) >> 1]);
				}
			}

			k = (vi & 1) + (hi & 1);
			recip(t1[0], yz[k], 0);
			mul(t1[1], yx[k], t1[0]);

			pack(t1[1], Y);
		}
		
		/////////////////////////////////////////////////////////////////////////// 
		
		/* sahn0:
		 * Using this class instead of long[10] to avoid bounds checks. */
		private static final class long10 {
			public long10() {}
			public long10(
				long _0, long _1, long _2, long _3, long _4,
				long _5, long _6, long _7, long _8, long _9)
			{
				this._0=_0; this._1=_1; this._2=_2;
				this._3=_3; this._4=_4; this._5=_5;
				this._6=_6; this._7=_7; this._8=_8;
				this._9=_9;
			}
			public long _0,_1,_2,_3,_4,_5,_6,_7,_8,_9;
		}
		
		/********************* radix 2^8 math *********************/
		
		private static final void cpy32(byte[] d, byte[] s) {
			int i;
			for (i = 0; i < 32; i++)
				d[i] = s[i];
		}
		
		/* p[m..n+m-1] = q[m..n+m-1] + z * x */
		/* n is the size of x */
		/* n+m is the size of p and q */
		private static final int mula_small(byte[] p,byte[] q,int m,byte[] x,int n,int z) {
			int v=0;
			for (int i=0;i<n;++i) {
				v+=(q[i+m] & 0xFF)+z*(x[i] & 0xFF);
				p[i+m]=(byte)v;
				v>>=8;
			}
			return v;		
		}
		
		/* p += x * y * z  where z is a small integer
		 * x is size 32, y is size t, p is size 32+t
		 * y is allowed to overlap with p+32 if you don't care about the upper half  */
		private static final int mula32(byte[] p, byte[] x, byte[] y, int t, int z) {
			final int n = 31;
			int w = 0;
			int i = 0;
			for (; i < t; i++) {
				int zy = z * (y[i] & 0xFF);
				w += mula_small(p, p, i, x, n, zy) +
					(p[i+n] & 0xFF) + zy * (x[n] & 0xFF);
				p[i+n] = (byte)w;
				w >>= 8;
			}
			p[i+n] = (byte)(w + (p[i+n] & 0xFF));
			return w >> 8;
		}
		
		/* divide r (size n) by d (size t), returning quotient q and remainder r
		 * quotient is size n-t+1, remainder is size t
		 * requires t > 0 && d[t-1] != 0
		 * requires that r[-1] and d[-1] are valid memory locations
		 * q may overlap with r+t */
		private static final void divmod(byte[] q, byte[] r, int n, byte[] d, int t) {
			int rn = 0;
			int dt = ((d[t-1] & 0xFF) << 8);
			if (t>1) {
				dt |= (d[t-2] & 0xFF);
			}
			while (n-- >= t) {
				int z = (rn << 16) | ((r[n] & 0xFF) << 8);
				if (n>0) {
					z |= (r[n-1] & 0xFF);
				}
				z/=dt;
				rn += mula_small(r,r, n-t+1, d, t, -z);
				q[n-t+1] = (byte)((z + rn) & 0xFF); /* rn is 0 or -1 (underflow) */
				mula_small(r,r, n-t+1, d, t, -rn);
				rn = (r[n] & 0xFF);
				r[n] = 0;
			}
			r[t-1] = (byte)rn;
		}
		
		private static final int numsize(byte[] x,int n) {
			while (n--!=0 && x[n]==0)
				;
			return n+1;
		}
		
		/* Returns x if a contains the gcd, y if b.
		 * Also, the returned buffer contains the inverse of a mod b,
		 * as 32-byte signed.
		 * x and y must have 64 bytes space for temporary use.
		 * requires that a[-1] and b[-1] are valid memory locations  */
		private static final byte[] egcd32(byte[] x,byte[] y,byte[] a,byte[] b) {
			int an, bn = 32, qn, i;
			for (i = 0; i < 32; i++)
				x[i] = y[i] = 0;
			x[0] = 1;
			an = numsize(a, 32);
			if (an==0)
				return y;	/* division by zero */
			byte[] temp=new byte[32];
			while (true) {
				qn = bn - an + 1;
				divmod(temp, b, bn, a, an);
				bn = numsize(b, bn);
				if (bn==0)
					return x;
				mula32(y, x, temp, qn, -1);

				qn = an - bn + 1;
				divmod(temp, a, an, b, bn);
				an = numsize(a, an);
				if (an==0)
					return y;
				mula32(x, y, temp, qn, -1);
			}
		}
		
		/********************* radix 2^25.5 GF(2^255-19) math *********************/	
		
		private static final int P25=33554431;	/* (1 << 25) - 1 */
		private static final int P26=67108863;	/* (1 << 26) - 1 */
		
		/* Convert to internal format from little-endian byte format */
		private static final void unpack(long10 x,byte[] m) {
			x._0 = ((m[0] & 0xFF))         | ((m[1] & 0xFF))<<8 |
					(m[2] & 0xFF)<<16      | ((m[3] & 0xFF)& 3)<<24;
			x._1 = ((m[3] & 0xFF)&~ 3)>>2  | (m[4] & 0xFF)<<6 |
					(m[5] & 0xFF)<<14 | ((m[6] & 0xFF)& 7)<<22;
			x._2 = ((m[6] & 0xFF)&~ 7)>>3  | (m[7] & 0xFF)<<5 |
					(m[8] & 0xFF)<<13 | ((m[9] & 0xFF)&31)<<21;
			x._3 = ((m[9] & 0xFF)&~31)>>5  | (m[10] & 0xFF)<<3 |
					(m[11] & 0xFF)<<11 | ((m[12] & 0xFF)&63)<<19;
			x._4 = ((m[12] & 0xFF)&~63)>>6 | (m[13] & 0xFF)<<2 |
					(m[14] & 0xFF)<<10 |  (m[15] & 0xFF)    <<18;
			x._5 =  (m[16] & 0xFF)         | (m[17] & 0xFF)<<8 |
					(m[18] & 0xFF)<<16 | ((m[19] & 0xFF)& 1)<<24;
			x._6 = ((m[19] & 0xFF)&~ 1)>>1 | (m[20] & 0xFF)<<7 |
					(m[21] & 0xFF)<<15 | ((m[22] & 0xFF)& 7)<<23;
			x._7 = ((m[22] & 0xFF)&~ 7)>>3 | (m[23] & 0xFF)<<5 |
					(m[24] & 0xFF)<<13 | ((m[25] & 0xFF)&15)<<21;
			x._8 = ((m[25] & 0xFF)&~15)>>4 | (m[26] & 0xFF)<<4 |
					(m[27] & 0xFF)<<12 | ((m[28] & 0xFF)&63)<<20;
			x._9 = ((m[28] & 0xFF)&~63)>>6 | (m[29] & 0xFF)<<2 |
					(m[30] & 0xFF)<<10 |  (m[31] & 0xFF)    <<18;
		}
		
		/* Check if reduced-form input >= 2^255-19 */
		private static final boolean is_overflow(long10 x) {
			return (
				((x._0 > P26-19)) &&
				((x._1 & x._3 & x._5 & x._7 & x._9) == P25) &&
				((x._2 & x._4 & x._6 & x._8) == P26)
				) || (x._9 > P25);
		}
		
		/* Convert from internal format to little-endian byte format.  The 
		 * number must be in a reduced form which is output by the following ops:
		 *     unpack, mul, sqr
		 *     set --  if input in range 0 .. P25
		 * If you're unsure if the number is reduced, first multiply it by 1.  */
		private static final void pack(long10 x,byte[] m) {
			int ld = 0, ud = 0;
			long t;
			ld = (is_overflow(x)?1:0) - ((x._9 < 0)?1:0);
			ud = ld * -(P25+1);
			ld *= 19;
			t = ld + x._0 + (x._1 << 26);
			m[ 0] = (byte)t;
			m[ 1] = (byte)(t >> 8);
			m[ 2] = (byte)(t >> 16);
			m[ 3] = (byte)(t >> 24);
			t = (t >> 32) + (x._2 << 19);
			m[ 4] = (byte)t;
			m[ 5] = (byte)(t >> 8);
			m[ 6] = (byte)(t >> 16);
			m[ 7] = (byte)(t >> 24);
			t = (t >> 32) + (x._3 << 13);
			m[ 8] = (byte)t;
			m[ 9] = (byte)(t >> 8);
			m[10] = (byte)(t >> 16);
			m[11] = (byte)(t >> 24);
			t = (t >> 32) + (x._4 <<  6);
			m[12] = (byte)t;
			m[13] = (byte)(t >> 8);
			m[14] = (byte)(t >> 16);
			m[15] = (byte)(t >> 24);
			t = (t >> 32) + x._5 + (x._6 << 25);
			m[16] = (byte)t;
			m[17] = (byte)(t >> 8);
			m[18] = (byte)(t >> 16);
			m[19] = (byte)(t >> 24);
			t = (t >> 32) + (x._7 << 19);
			m[20] = (byte)t;
			m[21] = (byte)(t >> 8);
			m[22] = (byte)(t >> 16);
			m[23] = (byte)(t >> 24);
			t = (t >> 32) + (x._8 << 12);
			m[24] = (byte)t;
			m[25] = (byte)(t >> 8);
			m[26] = (byte)(t >> 16);
			m[27] = (byte)(t >> 24);
			t = (t >> 32) + ((x._9 + ud) << 6);
			m[28] = (byte)t;
			m[29] = (byte)(t >> 8);
			m[30] = (byte)(t >> 16);
			m[31] = (byte)(t >> 24);
		}

		/* Copy a number */
		private static final void cpy(long10 out, long10 in) {
			out._0=in._0;	out._1=in._1;
			out._2=in._2;	out._3=in._3;
			out._4=in._4;	out._5=in._5;
			out._6=in._6;	out._7=in._7;
			out._8=in._8;	out._9=in._9;		
		}

		/* Set a number to value, which must be in range -185861411 .. 185861411 */
		private static final void set(long10 out, int in) {
			out._0=in;	out._1=0;
			out._2=0;	out._3=0;
			out._4=0;	out._5=0;
			out._6=0;	out._7=0;
			out._8=0;	out._9=0;		
		}
		
		/* Add/subtract two numbers.  The inputs must be in reduced form, and the 
		 * output isn't, so to do another addition or subtraction on the output, 
		 * first multiply it by one to reduce it. */
		private static final void add(long10 xy, long10 x, long10 y) {
			xy._0 = x._0 + y._0;	xy._1 = x._1 + y._1;
			xy._2 = x._2 + y._2;	xy._3 = x._3 + y._3;
			xy._4 = x._4 + y._4;	xy._5 = x._5 + y._5;
			xy._6 = x._6 + y._6;	xy._7 = x._7 + y._7;
			xy._8 = x._8 + y._8;	xy._9 = x._9 + y._9;
		}
		private static final void sub(long10 xy, long10 x, long10 y) {
			xy._0 = x._0 - y._0;	xy._1 = x._1 - y._1;
			xy._2 = x._2 - y._2;	xy._3 = x._3 - y._3;
			xy._4 = x._4 - y._4;	xy._5 = x._5 - y._5;
			xy._6 = x._6 - y._6;	xy._7 = x._7 - y._7;
			xy._8 = x._8 - y._8;	xy._9 = x._9 - y._9;
		}
		
		/* Multiply a number by a small integer in range -185861411 .. 185861411.
		 * The output is in reduced form, the input x need not be.  x and xy may point
		 * to the same buffer. */
		private static final long10 mul_small(long10 xy, long10 x, long y) {
			long t;
			t = (x._8*y);
			xy._8 = (t & ((1 << 26) - 1));
			t = (t >> 26) + (x._9*y);
			xy._9 = (t & ((1 << 25) - 1));
			t = 19 * (t >> 25) + (x._0*y);
			xy._0 = (t & ((1 << 26) - 1));
			t = (t >> 26) + (x._1*y);
			xy._1 = (t & ((1 << 25) - 1));
			t = (t >> 25) + (x._2*y);
			xy._2 = (t & ((1 << 26) - 1));
			t = (t >> 26) + (x._3*y);
			xy._3 = (t & ((1 << 25) - 1));
			t = (t >> 25) + (x._4*y);
			xy._4 = (t & ((1 << 26) - 1));
			t = (t >> 26) + (x._5*y);
			xy._5 = (t & ((1 << 25) - 1));
			t = (t >> 25) + (x._6*y);
			xy._6 = (t & ((1 << 26) - 1));
			t = (t >> 26) + (x._7*y);
			xy._7 = (t & ((1 << 25) - 1));
			t = (t >> 25) + xy._8;
			xy._8 = (t & ((1 << 26) - 1));
			xy._9 += (t >> 26);
			return xy;
		}

		/* Multiply two numbers.  The output is in reduced form, the inputs need not 
		 * be. */
		private static final long10 mul(long10 xy, long10 x, long10 y) {
			/* sahn0:
			 * Using local variables to avoid class access.
			 * This seem to improve performance a bit...
			 */
			long
				x_0=x._0,x_1=x._1,x_2=x._2,x_3=x._3,x_4=x._4,
				x_5=x._5,x_6=x._6,x_7=x._7,x_8=x._8,x_9=x._9;
			long
				y_0=y._0,y_1=y._1,y_2=y._2,y_3=y._3,y_4=y._4,
				y_5=y._5,y_6=y._6,y_7=y._7,y_8=y._8,y_9=y._9;
			long t;
			t = (x_0*y_8) + (x_2*y_6) + (x_4*y_4) + (x_6*y_2) +
				(x_8*y_0) + 2 * ((x_1*y_7) + (x_3*y_5) +
						(x_5*y_3) + (x_7*y_1)) + 38 *
				(x_9*y_9);
			xy._8 = (t & ((1 << 26) - 1));
			t = (t >> 26) + (x_0*y_9) + (x_1*y_8) + (x_2*y_7) +
				(x_3*y_6) + (x_4*y_5) + (x_5*y_4) +
				(x_6*y_3) + (x_7*y_2) + (x_8*y_1) +
				(x_9*y_0);
			xy._9 = (t & ((1 << 25) - 1));
			t = (x_0*y_0) + 19 * ((t >> 25) + (x_2*y_8) + (x_4*y_6)
					+ (x_6*y_4) + (x_8*y_2)) + 38 *
				((x_1*y_9) + (x_3*y_7) + (x_5*y_5) +
				 (x_7*y_3) + (x_9*y_1));
			xy._0 = (t & ((1 << 26) - 1));
			t = (t >> 26) + (x_0*y_1) + (x_1*y_0) + 19 * ((x_2*y_9)
					+ (x_3*y_8) + (x_4*y_7) + (x_5*y_6) +
					(x_6*y_5) + (x_7*y_4) + (x_8*y_3) +
					(x_9*y_2));
			xy._1 = (t & ((1 << 25) - 1));
			t = (t >> 25) + (x_0*y_2) + (x_2*y_0) + 19 * ((x_4*y_8)
					+ (x_6*y_6) + (x_8*y_4)) + 2 * (x_1*y_1)
					+ 38 * ((x_3*y_9) + (x_5*y_7) +
							(x_7*y_5) + (x_9*y_3));
			xy._2 = (t & ((1 << 26) - 1));
			t = (t >> 26) + (x_0*y_3) + (x_1*y_2) + (x_2*y_1) +
				(x_3*y_0) + 19 * ((x_4*y_9) + (x_5*y_8) +
						(x_6*y_7) + (x_7*y_6) +
						(x_8*y_5) + (x_9*y_4));
			xy._3 = (t & ((1 << 25) - 1));
			t = (t >> 25) + (x_0*y_4) + (x_2*y_2) + (x_4*y_0) + 19 *
				((x_6*y_8) + (x_8*y_6)) + 2 * ((x_1*y_3) +
									 (x_3*y_1)) + 38 *
				((x_5*y_9) + (x_7*y_7) + (x_9*y_5));
			xy._4 = (t & ((1 << 26) - 1));
			t = (t >> 26) + (x_0*y_5) + (x_1*y_4) + (x_2*y_3) +
				(x_3*y_2) + (x_4*y_1) + (x_5*y_0) + 19 *
				((x_6*y_9) + (x_7*y_8) + (x_8*y_7) +
				 (x_9*y_6));
			xy._5 = (t & ((1 << 25) - 1));
			t = (t >> 25) + (x_0*y_6) + (x_2*y_4) + (x_4*y_2) +
				(x_6*y_0) + 19 * (x_8*y_8) + 2 * ((x_1*y_5) +
						(x_3*y_3) + (x_5*y_1)) + 38 *
				((x_7*y_9) + (x_9*y_7));
			xy._6 = (t & ((1 << 26) - 1));
			t = (t >> 26) + (x_0*y_7) + (x_1*y_6) + (x_2*y_5) +
				(x_3*y_4) + (x_4*y_3) + (x_5*y_2) +
				(x_6*y_1) + (x_7*y_0) + 19 * ((x_8*y_9) +
						(x_9*y_8));
			xy._7 = (t & ((1 << 25) - 1));
			t = (t >> 25) + xy._8;
			xy._8 = (t & ((1 << 26) - 1));
			xy._9 += (t >> 26);
			return xy;
		}

		/* Square a number.  Optimization of  mul25519(x2, x, x)  */
		private static final long10 sqr(long10 x2, long10 x) {
			long
				x_0=x._0,x_1=x._1,x_2=x._2,x_3=x._3,x_4=x._4,
				x_5=x._5,x_6=x._6,x_7=x._7,x_8=x._8,x_9=x._9;
			long t;
			t = (x_4*x_4) + 2 * ((x_0*x_8) + (x_2*x_6)) + 38 *
				(x_9*x_9) + 4 * ((x_1*x_7) + (x_3*x_5));
			x2._8 = (t & ((1 << 26) - 1));
			t = (t >> 26) + 2 * ((x_0*x_9) + (x_1*x_8) + (x_2*x_7) +
					(x_3*x_6) + (x_4*x_5));
			x2._9 = (t & ((1 << 25) - 1));
			t = 19 * (t >> 25) + (x_0*x_0) + 38 * ((x_2*x_8) +
					(x_4*x_6) + (x_5*x_5)) + 76 * ((x_1*x_9)
					+ (x_3*x_7));
			x2._0 = (t & ((1 << 26) - 1));
			t = (t >> 26) + 2 * (x_0*x_1) + 38 * ((x_2*x_9) +
					(x_3*x_8) + (x_4*x_7) + (x_5*x_6));
			x2._1 = (t & ((1 << 25) - 1));
			t = (t >> 25) + 19 * (x_6*x_6) + 2 * ((x_0*x_2) +
					(x_1*x_1)) + 38 * (x_4*x_8) + 76 *
					((x_3*x_9) + (x_5*x_7));
			x2._2 = (t & ((1 << 26) - 1));
			t = (t >> 26) + 2 * ((x_0*x_3) + (x_1*x_2)) + 38 *
				((x_4*x_9) + (x_5*x_8) + (x_6*x_7));
			x2._3 = (t & ((1 << 25) - 1));
			t = (t >> 25) + (x_2*x_2) + 2 * (x_0*x_4) + 38 *
				((x_6*x_8) + (x_7*x_7)) + 4 * (x_1*x_3) + 76 *
				(x_5*x_9);
			x2._4 = (t & ((1 << 26) - 1));
			t = (t >> 26) + 2 * ((x_0*x_5) + (x_1*x_4) + (x_2*x_3))
				+ 38 * ((x_6*x_9) + (x_7*x_8));
			x2._5 = (t & ((1 << 25) - 1));
			t = (t >> 25) + 19 * (x_8*x_8) + 2 * ((x_0*x_6) +
					(x_2*x_4) + (x_3*x_3)) + 4 * (x_1*x_5) +
					76 * (x_7*x_9);
			x2._6 = (t & ((1 << 26) - 1));
			t = (t >> 26) + 2 * ((x_0*x_7) + (x_1*x_6) + (x_2*x_5) +
					(x_3*x_4)) + 38 * (x_8*x_9);
			x2._7 = (t & ((1 << 25) - 1));
			t = (t >> 25) + x2._8;
			x2._8 = (t & ((1 << 26) - 1));
			x2._9 += (t >> 26);
			return x2;
		}
		
		/* Calculates a reciprocal.  The output is in reduced form, the inputs need not 
		 * be.  Simply calculates  y = x^(p-2)  so it's not too fast. */
		/* When sqrtassist is true, it instead calculates y = x^((p-5)/8) */
		private static final void recip(long10 y, long10 x, int sqrtassist) {
			long10
			    t0=new long10(),
				t1=new long10(),
				t2=new long10(),
				t3=new long10(),
				t4=new long10();
			int i;
			/* the chain for x^(2^255-21) is straight from djb's implementation */
			sqr(t1, x);	/*  2 == 2 * 1	*/
			sqr(t2, t1);	/*  4 == 2 * 2	*/
			sqr(t0, t2);	/*  8 == 2 * 4	*/
			mul(t2, t0, x);	/*  9 == 8 + 1	*/
			mul(t0, t2, t1);	/* 11 == 9 + 2	*/
			sqr(t1, t0);	/* 22 == 2 * 11	*/
			mul(t3, t1, t2);	/* 31 == 22 + 9
						== 2^5   - 2^0	*/
			sqr(t1, t3);	/* 2^6   - 2^1	*/
			sqr(t2, t1);	/* 2^7   - 2^2	*/
			sqr(t1, t2);	/* 2^8   - 2^3	*/
			sqr(t2, t1);	/* 2^9   - 2^4	*/
			sqr(t1, t2);	/* 2^10  - 2^5	*/
			mul(t2, t1, t3);	/* 2^10  - 2^0	*/
			sqr(t1, t2);	/* 2^11  - 2^1	*/
			sqr(t3, t1);	/* 2^12  - 2^2	*/
			for (i = 1; i < 5; i++) {
				sqr(t1, t3);
				sqr(t3, t1);
			} /* t3 */		/* 2^20  - 2^10	*/
			mul(t1, t3, t2);	/* 2^20  - 2^0	*/
			sqr(t3, t1);	/* 2^21  - 2^1	*/
			sqr(t4, t3);	/* 2^22  - 2^2	*/
			for (i = 1; i < 10; i++) {
				sqr(t3, t4);
				sqr(t4, t3);
			} /* t4 */		/* 2^40  - 2^20	*/
			mul(t3, t4, t1);	/* 2^40  - 2^0	*/
			for (i = 0; i < 5; i++) {
				sqr(t1, t3);
				sqr(t3, t1);
			} /* t3 */		/* 2^50  - 2^10	*/
			mul(t1, t3, t2);	/* 2^50  - 2^0	*/
			sqr(t2, t1);	/* 2^51  - 2^1	*/
			sqr(t3, t2);	/* 2^52  - 2^2	*/
			for (i = 1; i < 25; i++) {
				sqr(t2, t3);
				sqr(t3, t2);
			} /* t3 */		/* 2^100 - 2^50 */
			mul(t2, t3, t1);	/* 2^100 - 2^0	*/
			sqr(t3, t2);	/* 2^101 - 2^1	*/
			sqr(t4, t3);	/* 2^102 - 2^2	*/
			for (i = 1; i < 50; i++) {
				sqr(t3, t4);
				sqr(t4, t3);
			} /* t4 */		/* 2^200 - 2^100 */
			mul(t3, t4, t2);	/* 2^200 - 2^0	*/
			for (i = 0; i < 25; i++) {
				sqr(t4, t3);
				sqr(t3, t4);
			} /* t3 */		/* 2^250 - 2^50	*/
			mul(t2, t3, t1);	/* 2^250 - 2^0	*/
			sqr(t1, t2);	/* 2^251 - 2^1	*/
			sqr(t2, t1);	/* 2^252 - 2^2	*/
			if (sqrtassist!=0) {
				mul(y, x, t2);	/* 2^252 - 3 */
			} else {
				sqr(t1, t2);	/* 2^253 - 2^3	*/
				sqr(t2, t1);	/* 2^254 - 2^4	*/
				sqr(t1, t2);	/* 2^255 - 2^5	*/
				mul(y, t1, t0);	/* 2^255 - 21	*/
			}
		}
		
		/* checks if x is "negative", requires reduced input */
		private static final int is_negative(long10 x) {
			return (int)(((is_overflow(x) || (x._9 < 0))?1:0) ^ (x._0 & 1));
		}
		
		/* a square root */
		private static final void sqrt(long10 x, long10 u) {
			long10 v=new long10(), t1=new long10(), t2=new long10();
			add(t1, u, u);	/* t1 = 2u		*/
			recip(v, t1, 1);	/* v = (2u)^((p-5)/8)	*/
			sqr(x, v);		/* x = v^2		*/
			mul(t2, t1, x);	/* t2 = 2uv^2		*/
			t2._0--;		/* t2 = 2uv^2-1		*/
			mul(t1, v, t2);	/* t1 = v(2uv^2-1)	*/
			mul(x, u, t1);	/* x = uv(2uv^2-1)	*/
		}
		
		/********************* Elliptic curve *********************/
		
		/* y^2 = x^3 + 486662 x^2 + x  over GF(2^255-19) */
		
		/* t1 = ax + az
		 * t2 = ax - az  */
		private static final void mont_prep(long10 t1, long10 t2, long10 ax, long10 az) {
			add(t1, ax, az);
			sub(t2, ax, az);
		}
		
		/* A = P + Q   where
		 *  X(A) = ax/az
		 *  X(P) = (t1+t2)/(t1-t2)
		 *  X(Q) = (t3+t4)/(t3-t4)
		 *  X(P-Q) = dx
		 * clobbers t1 and t2, preserves t3 and t4  */
		private static final void mont_add(long10 t1, long10 t2, long10 t3, long10 t4,long10 ax, long10 az, long10 dx) {
			mul(ax, t2, t3);
			mul(az, t1, t4);
			add(t1, ax, az);
			sub(t2, ax, az);
			sqr(ax, t1);
			sqr(t1, t2);
			mul(az, t1, dx);
		}
		
		/* B = 2 * Q   where
		 *  X(B) = bx/bz
		 *  X(Q) = (t3+t4)/(t3-t4)
		 * clobbers t1 and t2, preserves t3 and t4  */
		private static final void mont_dbl(long10 t1, long10 t2, long10 t3, long10 t4,long10 bx, long10 bz) {
			sqr(t1, t3);
			sqr(t2, t4);
			mul(bx, t1, t2);
			sub(t2, t1, t2);
			mul_small(bz, t2, 121665);
			add(t1, t1, bz);
			mul(bz, t1, t2);
		}
		
		/* Y^2 = X^3 + 486662 X^2 + X
		 * t is a temporary  */
		private static final void x_to_y2(long10 t, long10 y2, long10 x) {
			sqr(t, x);
			mul_small(y2, x, 486662);
			add(t, t, y2);
			t._0++;
			mul(y2, t, x);
		}
		
		/* P = kG   and  s = sign(P)/k  */
		private static final void core(byte[] Px, byte[] s, byte[] k, byte[] Gx) {
			long10 
			    dx=new long10(),
			    t1=new long10(),
			    t2=new long10(),
			    t3=new long10(),
			    t4=new long10();
		    long10[] 
		        x=new long10[]{new long10(),new long10()},
		    	z=new long10[]{new long10(),new long10()};
			int i, j;

			/* unpack the base */
			if (Gx!=null)
				unpack(dx, Gx);
			else
				set(dx, 9);

			/* 0G = point-at-infinity */
			set(x[0], 1);
			set(z[0], 0);

			/* 1G = G */
			cpy(x[1], dx);
			set(z[1], 1);

			for (i = 32; i--!=0; ) {
				if (i==0) {
					i=0;
				}
				for (j = 8; j--!=0; ) {
					/* swap arguments depending on bit */
					int bit1 = (k[i] & 0xFF) >> j & 1;
					int bit0 = ~(k[i] & 0xFF) >> j & 1;
					long10 ax = x[bit0];
					long10 az = z[bit0];
					long10 bx = x[bit1];
					long10 bz = z[bit1];

					/* a' = a + b	*/
					/* b' = 2 b	*/
					mont_prep(t1, t2, ax, az);
					mont_prep(t3, t4, bx, bz);
					mont_add(t1, t2, t3, t4, ax, az, dx);
					mont_dbl(t1, t2, t3, t4, bx, bz);
				}
			}

			recip(t1, z[0], 0);
			mul(dx, x[0], t1);
			pack(dx, Px);

			/* calculate s such that s abs(P) = G  .. assumes G is std base point */
			if (s!=null) {
				x_to_y2(t2, t1, dx);	/* t1 = Py^2  */
				recip(t3, z[1], 0);	/* where Q=P+G ... */
				mul(t2, x[1], t3);	/* t2 = Qx  */
				add(t2, t2, dx);	/* t2 = Qx + Px  */
				t2._0 += 9 + 486662;	/* t2 = Qx + Px + Gx + 486662  */
				dx._0 -= 9;		/* dx = Px - Gx  */
				sqr(t3, dx);	/* t3 = (Px - Gx)^2  */
				mul(dx, t2, t3);	/* dx = t2 (Px - Gx)^2  */
				sub(dx, dx, t1);	/* dx = t2 (Px - Gx)^2 - Py^2  */
				dx._0 -= 39420360;	/* dx = t2 (Px - Gx)^2 - Py^2 - Gy^2  */
				mul(t1, dx, BASE_R2Y);	/* t1 = -Py  */
				if (is_negative(t1)!=0)	/* sign is 1, so just copy  */
					cpy32(s, k);
				else			/* sign is -1, so negate  */
					mula_small(s, ORDER_TIMES_8, 0, k, 32, -1);

				/* reduce s mod q
				 * (is this needed?  do it just in case, it's fast anyway) */
				//divmod((dstptr) t1, s, 32, order25519, 32);

				/* take reciprocal of s mod q */
				byte[] temp1=new byte[32];
				byte[] temp2=new byte[64];
				byte[] temp3=new byte[64];
				cpy32(temp1, ORDER);
				cpy32(s, egcd32(temp2, temp3, s, temp1));
				if ((s[31] & 0x80)!=0)
					mula_small(s, s, 0, ORDER, 32, 1);
			}
		}

		/* smallest multiple of the order that's >= 2^255 */
		private static final byte[] ORDER_TIMES_8 = {
			(byte)104, (byte)159, (byte)174, (byte)231,
			(byte)210, (byte)24,  (byte)147, (byte)192,
			(byte)178, (byte)230, (byte)188, (byte)23,
			(byte)245, (byte)206, (byte)247, (byte)166,
			(byte)0,   (byte)0,   (byte)0,   (byte)0,  
			(byte)0,   (byte)0,   (byte)0,   (byte)0,
			(byte)0,   (byte)0,   (byte)0,   (byte)0,  
			(byte)0,   (byte)0,   (byte)0,   (byte)128
		};
		
		/* constants 2Gy and 1/(2Gy) */
		private static final long10 BASE_2Y = new long10(
			39999547, 18689728, 59995525, 1648697, 57546132,
			24010086, 19059592, 5425144, 63499247, 16420658
		);
		private static final long10 BASE_R2Y = new long10(
			5744, 8160848, 4790893, 13779497, 35730846,
			12541209, 49101323, 30047407, 40071253, 6226132
		);
	}
	
	static class Peer implements Comparable<Peer> {
		
		static final int STATE_NONCONNECTED = 0;
		static final int STATE_CONNECTED = 1;
		static final int STATE_DISCONNECTED = 2;
		
		final int index;
		String platform;
		String scheme;
		int port;
		String announcedAddress;
		boolean shareAddress;
		String hallmark;
		long accountId;
		int weight, date;
		long adjustedWeight;
		String application, version;
		
		long blacklistingTime;
		int state;
		long downloadedVolume, uploadedVolume;
		
		Peer(String announcedAddress, int index) {
			
			this.announcedAddress = announcedAddress;
			this.index = index;

		}
		
		static Peer addPeer(String address, String announcedAddress) {
			
			try {
				
				new URL("http://" + address);
				
			} catch (Exception e) {
				
				return null;
				
			}
			try {
				
				new URL("http://" + announcedAddress);
				
			} catch (Exception e) {
				
				announcedAddress = "";
				
			}
			
			if (address.equals("localhost") || address.equals("127.0.0.1") || address.equals("0:0:0:0:0:0:0:1")) {
				
				return null;
				
			}

            if (myAddress != null && myAddress.length() > 0 && myAddress.equals(announcedAddress)) {

                return null;

            }

            Peer peer = peers.get(announcedAddress.length() > 0 ? announcedAddress : address);
            if (peer == null) {

                //TODO: Check addresses

                peer = new Peer(announcedAddress, peerCounter.incrementAndGet());
                peers.put(announcedAddress.length() > 0 ? announcedAddress : address, peer);

            }

            return peer;

		}
		
		boolean analyzeHallmark(String realHost, String hallmark) {
			
			if (hallmark == null) {
				
				return true;
				
			}
			
			try {
				
				byte[] hallmarkBytes = convert(hallmark);
				
				ByteBuffer buffer = ByteBuffer.wrap(hallmarkBytes);
				buffer.order(ByteOrder.LITTLE_ENDIAN);
				
				byte[] publicKey = new byte[32];
				buffer.get(publicKey);
				int hostLength = buffer.getShort();
				byte[] hostBytes = new byte[hostLength];
				buffer.get(hostBytes);
				String host = new String(hostBytes, "UTF-8");
				if (host.length() > 100 || !host.equals(realHost)) {
					
					return false;
					
				}
				int weight = buffer.getInt();
				if (weight <= 0 || weight > MAX_BALANCE) {
					
					return false;
					
				}
				int date = buffer.getInt();
				buffer.get();
				byte[] signature = new byte[64];
				buffer.get(signature);
				
				byte[] data = new byte[hallmarkBytes.length - 64];
				System.arraycopy(hallmarkBytes, 0, data, 0, data.length);
				
				if (Crypto.verify(signature, data, publicKey)) {
					
					this.hallmark = hallmark;
					
					long accountId = Account.getId(publicKey);
					Account account = accounts.get(accountId);
					if (account == null) {
						
						return false;
						
					}
					LinkedList<Peer> groupedPeers = new LinkedList<>();
					int validDate = 0;

                    this.accountId = accountId;
                    this.weight = weight;
                    this.date = date;

                    for (Peer peer : peers.values()) {

                        if (peer.accountId == accountId) {

                            groupedPeers.add(peer);
                            if (peer.date > validDate) {

                                validDate = peer.date;

                            }

                        }

                    }

                    long totalWeight = 0;
                    for (Peer peer : groupedPeers) {

                        if (peer.date == validDate) {

                            totalWeight += peer.weight;

                        } else {

                            peer.adjustedWeight = 0;
                            peer.updateWeight();

                        }

                    }

                    for (Peer peer : groupedPeers) {

                        peer.adjustedWeight = MAX_BALANCE * peer.weight / totalWeight;
                        peer.updateWeight();

                    }

					return true;
					
				}
				
			} catch (Exception e) { }

			return false;
			
		}
		
		void blacklist() {
			
			blacklistingTime = System.currentTimeMillis();
			
			JSONObject response = new JSONObject();
			response.put("response", "processNewData");
			
			JSONArray removedKnownPeers = new JSONArray();
			JSONObject removedKnownPeer = new JSONObject();
			removedKnownPeer.put("index", index);
			removedKnownPeers.add(removedKnownPeer);
			response.put("removedKnownPeers", removedKnownPeers);
			
			JSONArray addedBlacklistedPeers = new JSONArray();
			JSONObject addedBlacklistedPeer = new JSONObject();
			addedBlacklistedPeer.put("index", index);
			addedBlacklistedPeer.put("announcedAddress", announcedAddress.length() > 30 ? (announcedAddress.substring(0, 30) + "...") : announcedAddress);
			for (String wellKnownPeer : wellKnownPeers) {
				
				if (announcedAddress.equals(wellKnownPeer)) {
					
					addedBlacklistedPeer.put("wellKnown", true);
					
					break;
					
				}
				
			}
			addedBlacklistedPeers.add(addedBlacklistedPeer);
			response.put("addedBlacklistedPeers", addedBlacklistedPeers);
			
			for (User user : users.values()) {
				
				user.send(response);
				
			}
			
		}
		
		@Override
		public int compareTo(Peer o) {
			
			long weight = getWeight(), weight2 = o.getWeight();
			if (weight > weight2) {
				
				return -1;
				
			} else if (weight < weight2) {
				
				return 1;
				
			} else {
				
				return index - o.index;
				
			}
			
		}
		
		void connect() {
			
			JSONObject request = new JSONObject();
			request.put("requestType", "getInfo");
			if (myAddress != null && myAddress.length() > 0) {
				
				request.put("announcedAddress", myAddress);
				
			}
			if (myHallmark != null && myHallmark.length() > 0) {
				
				request.put("hallmark", myHallmark);
				
			}
			request.put("application", "NRS");
			request.put("version", VERSION);
			request.put("platform", myPlatform);
			request.put("scheme", myScheme);
			request.put("port", myPort);
			request.put("shareAddress", shareMyAddress);
			JSONObject response = send(request);
			if (response != null) {
				
				application = (String)response.get("application");
				version = (String)response.get("version");
				platform = (String)response.get("platform");
				try {
					
					shareAddress = Boolean.parseBoolean((String)response.get("shareAddress"));
					
				} catch (Exception e) {
					
					/**/shareAddress = true;
					
				}
				
				if (analyzeHallmark(announcedAddress, (String)response.get("hallmark"))) {
					
					setState(STATE_CONNECTED);
					
				} else {
					
					blacklist();
					
				}
				
			}
			
		}
		
		void deactivate() {
			
			if (state == STATE_CONNECTED) {
				
				disconnect();
				
			}
			setState(STATE_NONCONNECTED);
			
			JSONObject response = new JSONObject();
			response.put("response", "processNewData");
			
			JSONArray removedActivePeers = new JSONArray();
			JSONObject removedActivePeer = new JSONObject();
			removedActivePeer.put("index", index);
			removedActivePeers.add(removedActivePeer);
			response.put("removedActivePeers", removedActivePeers);
			
			if (announcedAddress.length() > 0) {
				
				JSONArray addedKnownPeers = new JSONArray();
				JSONObject addedKnownPeer = new JSONObject();
				addedKnownPeer.put("index", index);
				addedKnownPeer.put("announcedAddress", announcedAddress.length() > 30 ? (announcedAddress.substring(0, 30) + "...") : announcedAddress);
				for (String wellKnownPeer : wellKnownPeers) {
					
					if (announcedAddress.equals(wellKnownPeer)) {
						
						addedKnownPeer.put("wellKnown", true);
						
						break;
						
					}
					
				}
				addedKnownPeers.add(addedKnownPeer);
				response.put("addedKnownPeers", addedKnownPeers);
				
			}
			
			for (User user : users.values()) {
				
				user.send(response);
				
			}
			
		}
		
		void disconnect() {
			
			setState(STATE_DISCONNECTED);
			
		}
		
		static Peer getAnyPeer(int state, boolean applyPullThreshold) {

            List<Peer> selectedPeers = new ArrayList<Peer>();

            for (Peer peer : Nxt.peers.values()) {

                if (peer.blacklistingTime <= 0 && peer.state == state && peer.announcedAddress.length() > 0
                        && (!applyPullThreshold || !enableHallmarkProtection || peer.getWeight() >= pullThreshold)) {

                    selectedPeers.add(peer);

                }

            }
            /*  was:

				Collection<Peer> peers = ((HashMap<String, Peer>)Nxt.peers.clone()).values();
				Iterator<Peer> iterator = peers.iterator();
				while (iterator.hasNext()) {
					
					Peer peer = iterator.next();
					if (peer.blacklistingTime > 0 || peer.state != state || peer.announcedAddress.length() == 0 || (applyPullThreshold && enableHallmarkProtection && peer.getWeight() < pullThreshold)) {
						
						iterator.remove();
						
					}
					
				}
			*/

			if (selectedPeers.size() > 0) {

                long totalWeight = 0;
                for (Peer peer : selectedPeers) {

                    long weight = peer.getWeight();
                    if (weight == 0) {

                        weight = 1;

                    }
                    totalWeight += weight;

                }

                long hit = ThreadLocalRandom.current().nextLong(totalWeight);
                for (Peer peer : selectedPeers) {

                    long weight = peer.getWeight();
                    if (weight == 0) {

                        weight = 1;

                    }
                    if ((hit -= weight) < 0) {

                        return peer;

                    }

                }

            }
				
            return null;

		}
		
		static int getNumberOfConnectedPublicPeers() {
			
			int numberOfConnectedPeers = 0;

            for (Peer peer : peers.values()) {

                if (peer.state == STATE_CONNECTED && peer.announcedAddress.length() > 0) {

                    numberOfConnectedPeers++;

                }

            }

			return numberOfConnectedPeers;
			
		}
		
		int getWeight() {
			
			if (accountId == 0) {
				
				return 0;
				
			}
			Account account = accounts.get(accountId);
			if (account == null) {
				
				return 0;
				
			}
			
			return (int)(adjustedWeight * (account.balance / 100) / MAX_BALANCE);
			
		}
		
		void removeBlacklistedStatus() {
			
			setState(STATE_NONCONNECTED);
			blacklistingTime = 0;
			
			JSONObject response = new JSONObject();
			response.put("response", "processNewData");
			
			JSONArray removedBlacklistedPeers = new JSONArray();
			JSONObject removedBlacklistedPeer = new JSONObject();
			removedBlacklistedPeer.put("index", index);
			removedBlacklistedPeers.add(removedBlacklistedPeer);
			response.put("removedBlacklistedPeers", removedBlacklistedPeers);
			
			JSONArray addedKnownPeers = new JSONArray();
			JSONObject addedKnownPeer = new JSONObject();
			addedKnownPeer.put("index", index);
			addedKnownPeer.put("announcedAddress", announcedAddress.length() > 30 ? (announcedAddress.substring(0, 30) + "...") : announcedAddress);
			for (String wellKnownPeer : wellKnownPeers) {
				
				if (announcedAddress.equals(wellKnownPeer)) {
					
					addedKnownPeer.put("wellKnown", true);
					
					break;
					
				}
				
			}
			addedKnownPeers.add(addedKnownPeer);
			response.put("addedKnownPeers", addedKnownPeers);
			
			for (User user : users.values()) {
				
				user.send(response);
				
			}
			
		}
		
		void removePeer() {

            peers.values().remove(this);

			JSONObject response = new JSONObject();
			response.put("response", "processNewData");
			
			JSONArray removedKnownPeers = new JSONArray();
			JSONObject removedKnownPeer = new JSONObject();
			removedKnownPeer.put("index", index);
			removedKnownPeers.add(removedKnownPeer);
			response.put("removedKnownPeers", removedKnownPeers);
			
			for (User user : users.values()) {
				
				user.send(response);
				
			}
			
		}

        //TODO: send in parallel using an executor service
		static void sendToAllPeers(JSONObject request) {

            for (Peer peer : Nxt.peers.values()) {

                if (enableHallmarkProtection && peer.getWeight() < pushThreshold) {
                    continue;
                }

                if (peer.blacklistingTime == 0 && peer.state == Peer.STATE_CONNECTED && peer.announcedAddress.length() > 0) {

                    peer.send(request);

                }

            }

            /* was:
            Peer[] peers;
			synchronized (Nxt.peers) {
				
				peers = Nxt.peers.values().toArray(new Peer[0]);
				
			}
			Arrays.sort(peers);
			for (Peer peer : peers) {
				
				if (enableHallmarkProtection && peer.getWeight() < pushThreshold) {
					
					break;
					
				}
				
				if (peer.blacklistingTime == 0 && peer.state == Peer.STATE_CONNECTED && peer.announcedAddress.length() > 0) {
					
					peer.send(request);
					
				}
				
			}
			*/
			
		}
		
		JSONObject send(JSONObject request) {
			
			JSONObject response;
			
			String log = null;
			boolean showLog = false;
			
			HttpURLConnection connection = null;
			
			try {
				
				if (communicationLoggingMask != 0) {
					
					log = "\"" + announcedAddress + "\": " + request.toString();
					
				}
				
				request.put("protocol", 1);
				
				/**/URL url = new URL("http://" + announcedAddress + ((new URL("http://" + announcedAddress)).getPort() < 0 ? ":7874" : "") + "/nxt");
				/**///URL url = new URL("http://" + announcedAddress + ":6874" + "/nxt");
				connection = (HttpURLConnection)url.openConnection();
				connection.setRequestMethod("POST");
				connection.setDoOutput(true);
				connection.setConnectTimeout(connectTimeout);
				connection.setReadTimeout(readTimeout);
				byte[] requestBytes = request.toString().getBytes("UTF-8");
                //TODO: use JSONObject.writeJSONString(writer) to skip intermediate byte[] creation
				try (OutputStream outputStream = connection.getOutputStream()) {
    				outputStream.write(requestBytes);
                }
				updateUploadedVolume(requestBytes.length);
				if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
					
					ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    byte[] buffer = new byte[65536];
                    int numberOfBytes;
                    try (InputStream inputStream = connection.getInputStream()) {
                        while ((numberOfBytes = inputStream.read(buffer)) > 0) {

                            byteArrayOutputStream.write(buffer, 0, numberOfBytes);

                        }
                    }
					String responseValue = byteArrayOutputStream.toString("UTF-8");
					if ((communicationLoggingMask & LOGGING_MASK_200_RESPONSES) != 0) {
						
						log += " >>> " + responseValue;
						showLog = true;
						
					}
					updateDownloadedVolume(responseValue.getBytes("UTF-8").length);
					//TODO: parse using a reader from the stream, skip String creation
					response = (JSONObject)JSONValue.parse(responseValue);
					
				} else {
					
					if ((communicationLoggingMask & LOGGING_MASK_NON200_RESPONSES) != 0) {
						
						log += " >>> Peer responded with HTTP " + connection.getResponseCode() + " code!";
						showLog = true;
						
					}
					
					disconnect();
					
					response = null;
					
				}
				
			} catch (Exception e) {
				
				if ((communicationLoggingMask & LOGGING_MASK_EXCEPTIONS) != 0) {
					
					log += " >>> " + e.toString();
					showLog = true;
					
				}
				
				if (state == STATE_NONCONNECTED) {
					
					blacklist();
					
				} else {
					
					disconnect();
					
				}
				
				response = null;

			}
			
			if (showLog) {
				
				logMessage(log + "\n");
				
			}
			
			if (connection != null) {
				
				connection.disconnect();
				
			}
			
			return response;
			
		}
		
		void setState(int state) {
			
			if (this.state == STATE_NONCONNECTED && state != STATE_NONCONNECTED) {
				
				JSONObject response = new JSONObject();
				response.put("response", "processNewData");
				
				if (announcedAddress.length() > 0) {
					
					JSONArray removedKnownPeers = new JSONArray();
					JSONObject removedKnownPeer = new JSONObject();
					removedKnownPeer.put("index", index);
					removedKnownPeers.add(removedKnownPeer);
					response.put("removedKnownPeers", removedKnownPeers);
					
				}
				
				JSONArray addedActivePeers = new JSONArray();
				JSONObject addedActivePeer = new JSONObject();
				addedActivePeer.put("index", index);
				if (state == STATE_DISCONNECTED) {
					
					addedActivePeer.put("disconnected", true);
					
				}

                //TODO: there must be a better way
				for (Map.Entry<String, Peer> peerEntry : peers.entrySet()) {
					
					if (peerEntry.getValue() == this) {
						
						addedActivePeer.put("address", peerEntry.getKey().length() > 30 ? (peerEntry.getKey().substring(0, 30) + "...") : peerEntry.getKey());
						
						break;
						
					}
					
				}
				addedActivePeer.put("announcedAddress", announcedAddress.length() > 30 ? (announcedAddress.substring(0, 30) + "...") : announcedAddress);
				addedActivePeer.put("weight", getWeight());
				addedActivePeer.put("downloaded", downloadedVolume);
				addedActivePeer.put("uploaded", uploadedVolume);
				addedActivePeer.put("software", (application == null ? "?" : application) + " (" + (version == null ? "?" : version) + ")" + " @ " + (platform == null ? "?" : platform));
				for (String wellKnownPeer : wellKnownPeers) {
					
					if (announcedAddress.equals(wellKnownPeer)) {
						
						addedActivePeer.put("wellKnown", true);
						
						break;
						
					}
					
				}
				addedActivePeers.add(addedActivePeer);
				response.put("addedActivePeers", addedActivePeers);
				
				for (User user : users.values()) {
					
					user.send(response);
					
				}
				
			} else if (this.state != STATE_NONCONNECTED && state != STATE_NONCONNECTED) {
				
				JSONObject response = new JSONObject();
				response.put("response", "processNewData");
				
				JSONArray changedActivePeers = new JSONArray();
				JSONObject changedActivePeer = new JSONObject();
				changedActivePeer.put("index", index);
				changedActivePeer.put(state == STATE_CONNECTED ? "connected" : "disconnected", true);
				changedActivePeers.add(changedActivePeer);
				response.put("changedActivePeers", changedActivePeers);
				
				for (User user : users.values()) {
					
					user.send(response);
					
				}
				
			}
			
			this.state = state;
			
		}
		
		void updateDownloadedVolume(int volume) {
			
			downloadedVolume += volume;
			
			JSONObject response = new JSONObject();
			response.put("response", "processNewData");
			
			JSONArray changedActivePeers = new JSONArray();
			JSONObject changedActivePeer = new JSONObject();
			changedActivePeer.put("index", index);
			changedActivePeer.put("downloaded", downloadedVolume);
			changedActivePeers.add(changedActivePeer);
			response.put("changedActivePeers", changedActivePeers);
			
			for (User user : users.values()) {
				
				user.send(response);
				
			}
			
		}
		
		void updateUploadedVolume(int volume) {
			
			uploadedVolume += volume;
			
			JSONObject response = new JSONObject();
			response.put("response", "processNewData");
			
			JSONArray changedActivePeers = new JSONArray();
			JSONObject changedActivePeer = new JSONObject();
			changedActivePeer.put("index", index);
			changedActivePeer.put("uploaded", uploadedVolume);
			changedActivePeers.add(changedActivePeer);
			response.put("changedActivePeers", changedActivePeers);
			
			for (User user : users.values()) {
				
				user.send(response);
				
			}
			
		}
		
		void updateWeight() {
			
			JSONObject response = new JSONObject();
			response.put("response", "processNewData");
			
			JSONArray changedActivePeers = new JSONArray();
			JSONObject changedActivePeer = new JSONObject();
			changedActivePeer.put("index", index);
			changedActivePeer.put("weight", getWeight());
			changedActivePeers.add(changedActivePeer);
			response.put("changedActivePeers", changedActivePeers);
			
			for (User user : users.values()) {
				
				user.send(response);
				
			}
			
		}
		
	}
	
	static class Transaction implements Comparable<Transaction>, Serializable {
		
		static final long serialVersionUID = 0;
		
		static final byte TYPE_PAYMENT = 0;
		static final byte TYPE_MESSAGING = 1;
		static final byte TYPE_COLORED_COINS = 2;
		
		static final byte SUBTYPE_PAYMENT_ORDINARY_PAYMENT = 0;
		
		static final byte SUBTYPE_MESSAGING_ARBITRARY_MESSAGE = 0;
		static final byte SUBTYPE_MESSAGING_ALIAS_ASSIGNMENT = 1;
		
		static final byte SUBTYPE_COLORED_COINS_ASSET_ISSUANCE = 0;
		static final byte SUBTYPE_COLORED_COINS_ASSET_TRANSFER = 1;
		static final byte SUBTYPE_COLORED_COINS_ASK_ORDER_PLACEMENT = 2;
		static final byte SUBTYPE_COLORED_COINS_BID_ORDER_PLACEMENT = 3;
		static final byte SUBTYPE_COLORED_COINS_ASK_ORDER_CANCELLATION = 4;
		static final byte SUBTYPE_COLORED_COINS_BID_ORDER_CANCELLATION = 5;
		
		static final int ASSET_ISSUANCE_FEE = 1000;
		
        //TODO: timestamp and signature currently not thread safe
		byte type, subtype;
		int timestamp;
		short deadline;
		byte[] senderPublicKey;
		long recipient;
		int amount, fee;
		long referencedTransaction;
		byte[] signature;
		Attachment attachment;
		
		int index;
        //TODO: volatile?
		long block;
		int height;
		
		Transaction(byte type, byte subtype, int timestamp, short deadline, byte[] senderPublicKey, long recipient, int amount, int fee, long referencedTransaction, byte[] signature) {
			
			this.type = type;
			this.subtype = subtype;
			this.timestamp = timestamp;
			this.deadline = deadline;
			this.senderPublicKey = senderPublicKey;
			this.recipient = recipient;
			this.amount = amount;
			this.fee = fee;
			this.referencedTransaction = referencedTransaction;
			this.signature = signature;
			
			height = Integer.MAX_VALUE;
			
		}
		
		@Override
		public int compareTo(Transaction o) {
			
			if (height < o.height) {
				
				return -1;
				
			} else if (height > o.height) {
				
				return 1;
				
			} else {
				//TODO: cache bytes or at least bytes.length
                //TODO: rewrite comparison, WTF is 1048576L ???
				if (fee * 1048576L / getBytes().length > o.fee * 1048576L / o.getBytes().length) {
					
					return -1;
					
				} else if (fee * 1048576L / getBytes().length < o.fee * 1048576L / o.getBytes().length) {
					
					return 1;
					
				} else {
					
					if (timestamp < o.timestamp) {
						
						return -1;
						
					} else if (timestamp > o.timestamp) {
						
						return 1;
						
					} else {
						
						if (index < o.index) {
							
							return -1;
							
						} else if (index > o.index) {
							
							return 1;
							
						} else {
							
							return 0;
							
						}
						
					}
					
				}
				
			}
			
		}
		
		byte[] getBytes() {
			
			ByteBuffer buffer = ByteBuffer.allocate(1 + 1 + 4 + 2 + 32 + 8 + 4 + 4 + 8 + 64 + (attachment == null ? 0 : attachment.getBytes().length));
			buffer.order(ByteOrder.LITTLE_ENDIAN);
			buffer.put(type);
			buffer.put(subtype);
			buffer.putInt(timestamp);
			buffer.putShort(deadline);
			buffer.put(senderPublicKey);
			buffer.putLong(recipient);
			buffer.putInt(amount);
			buffer.putInt(fee);
			buffer.putLong(referencedTransaction);
			buffer.put(signature);
			if (attachment != null) {
				
				buffer.put(attachment.getBytes());
				
			}
			
			return buffer.array();
			
		}
		
		long getId() throws Exception {
			
			byte[] hash = MessageDigest.getInstance("SHA-256").digest(getBytes());
			BigInteger bigInteger = new BigInteger(1, new byte[] {hash[7], hash[6], hash[5], hash[4], hash[3], hash[2], hash[1], hash[0]});
			return bigInteger.longValue();
			
		}
		
		JSONObject getJSONObject() {
			
			JSONObject transaction = new JSONObject();
			
			transaction.put("type", type);
			transaction.put("subtype", subtype);
			transaction.put("timestamp", timestamp);
			transaction.put("deadline", deadline);
			transaction.put("senderPublicKey", convert(senderPublicKey));
			transaction.put("recipient", convert(recipient));
			transaction.put("amount", amount);
			transaction.put("fee", fee);
			transaction.put("referencedTransaction", convert(referencedTransaction));
			transaction.put("signature", convert(signature));
			if (attachment != null) {
				
				transaction.put("attachment", attachment.getJSONObject());
				
			}
			
			return transaction;
			
		}
		
		static Transaction getTransaction(ByteBuffer buffer) {
			
			byte type = buffer.get();
			byte subtype = buffer.get();
			int timestamp = buffer.getInt();
			short deadline = buffer.getShort();
			byte[] senderPublicKey = new byte[32];
			buffer.get(senderPublicKey);
			long recipient = buffer.getLong();
			int amount = buffer.getInt();
			int fee = buffer.getInt();
			long referencedTransaction = buffer.getLong();
			byte[] signature = new byte[64];
			buffer.get(signature);
			
			Transaction transaction = new Transaction(type, subtype, timestamp, deadline, senderPublicKey, recipient, amount, fee, referencedTransaction, signature);
			
			switch (type) {
			
			case Transaction.TYPE_MESSAGING:
				{
					
					switch (subtype) {
					
					case Transaction.SUBTYPE_MESSAGING_ALIAS_ASSIGNMENT:
						{
							
							int aliasLength = buffer.get();
							byte[] alias = new byte[aliasLength];
							buffer.get(alias);
							int uriLength = buffer.getShort();
							byte[] uri = new byte[uriLength];
							buffer.get(uri);
							
							try {
								
								transaction.attachment = new Transaction.MessagingAliasAssignmentAttachment(new String(alias, "UTF-8"), new String(uri, "UTF-8"));
								
							} catch (Exception e) { }
							
						}
						break;
						
					}
					
				}
				break;
				
			case Transaction.TYPE_COLORED_COINS:
				{
					
					switch (subtype) {
					
					case Transaction.SUBTYPE_COLORED_COINS_ASSET_ISSUANCE:
						{
							
							int nameLength = buffer.get();
							byte[] name = new byte[nameLength];
							buffer.get(name);
							int descriptionLength = buffer.getShort();
							byte[] description = new byte[descriptionLength];
							buffer.get(description);
							int quantity = buffer.getInt();
							
							try {
								
								transaction.attachment = new Transaction.ColoredCoinsAssetIssuanceAttachment(new String(name, "UTF-8"), new String(description, "UTF-8"), quantity);
								
							} catch (Exception e) { }
							
						}
						break;
						
					case Transaction.SUBTYPE_COLORED_COINS_ASSET_TRANSFER:
						{
							
							long asset = buffer.getLong();
							int quantity = buffer.getInt();
							
							transaction.attachment = new Transaction.ColoredCoinsAssetTransferAttachment(asset, quantity);
							
						}
						break;
						
					case Transaction.SUBTYPE_COLORED_COINS_ASK_ORDER_PLACEMENT:
						{
							
							long asset = buffer.getLong();
							int quantity = buffer.getInt();
							long price = buffer.getLong();
							
							transaction.attachment = new Transaction.ColoredCoinsAskOrderPlacementAttachment(asset, quantity, price);
							
						}
						break;
						
					case Transaction.SUBTYPE_COLORED_COINS_BID_ORDER_PLACEMENT:
						{
							
							long asset = buffer.getLong();
							int quantity = buffer.getInt();
							long price = buffer.getLong();
							
							transaction.attachment = new Transaction.ColoredCoinsBidOrderPlacementAttachment(asset, quantity, price);
							
						}
						break;
						
					case Transaction.SUBTYPE_COLORED_COINS_ASK_ORDER_CANCELLATION:
						{
							
							long order = buffer.getLong();
							
							transaction.attachment = new Transaction.ColoredCoinsAskOrderCancellationAttachment(order);
							
						}
						break;
						
					case Transaction.SUBTYPE_COLORED_COINS_BID_ORDER_CANCELLATION:
						{
							
							long order = buffer.getLong();
							
							transaction.attachment = new Transaction.ColoredCoinsBidOrderCancellationAttachment(order);
							
						}
						break;
						
					}
					
				}
				break;
				
			}
			
			return transaction;
			
		}
		
        //TODO: refactor common code with the above getTransaction(ByteBuffer)
		static Transaction getTransaction(JSONObject transactionData) {
			
			byte type = ((Long)transactionData.get("type")).byteValue();
			byte subtype = ((Long)transactionData.get("subtype")).byteValue();
			int timestamp = ((Long)transactionData.get("timestamp")).intValue();
			short deadline = ((Long)transactionData.get("deadline")).shortValue();
			byte[] senderPublicKey = convert((String)transactionData.get("senderPublicKey"));
			long recipient = (new BigInteger((String)transactionData.get("recipient"))).longValue();
			int amount = ((Long)transactionData.get("amount")).intValue();
			int fee = ((Long)transactionData.get("fee")).intValue();
			long referencedTransaction = (new BigInteger((String)transactionData.get("referencedTransaction"))).longValue();
			byte[] signature = convert((String)transactionData.get("signature"));
			
			Transaction transaction = new Transaction(type, subtype, timestamp, deadline, senderPublicKey, recipient, amount, fee, referencedTransaction, signature);
			
			JSONObject attachmentData = (JSONObject)transactionData.get("attachment");
			switch (type) {
			
			case TYPE_MESSAGING:
				{
					
					switch (subtype) {
					
					case SUBTYPE_MESSAGING_ALIAS_ASSIGNMENT:
						{
							
							String alias = (String)attachmentData.get("alias");
							String uri = (String)attachmentData.get("uri");
							transaction.attachment = new MessagingAliasAssignmentAttachment(alias.trim(), uri.trim());
							
						}
						break;
						
					}
					
				}
				break;
				
			case TYPE_COLORED_COINS:
				{
					
					switch (subtype) {
					
					case SUBTYPE_COLORED_COINS_ASSET_ISSUANCE:
						{
							
							String name = (String)attachmentData.get("name");
							String description = (String)attachmentData.get("description");
							int quantity = ((Long)attachmentData.get("quantity")).intValue();
							transaction.attachment = new ColoredCoinsAssetIssuanceAttachment(name.trim(), description.trim(), quantity);
							
						}
						break;
						
					case SUBTYPE_COLORED_COINS_ASSET_TRANSFER:
						{
							
							long asset = (new BigInteger((String)attachmentData.get("asset"))).longValue();
							int quantity = ((Long)attachmentData.get("quantity")).intValue();
							transaction.attachment = new ColoredCoinsAssetTransferAttachment(asset, quantity);
							
						}
						break;
						
					case SUBTYPE_COLORED_COINS_ASK_ORDER_PLACEMENT:
						{
							
							long asset = (new BigInteger((String)attachmentData.get("asset"))).longValue();
							int quantity = ((Long)attachmentData.get("quantity")).intValue();
							long price = (Long)attachmentData.get("price");
							transaction.attachment = new ColoredCoinsAskOrderPlacementAttachment(asset, quantity, price);
							
						}
						break;
						
					case SUBTYPE_COLORED_COINS_BID_ORDER_PLACEMENT:
						{
							
							long asset = (new BigInteger((String)attachmentData.get("asset"))).longValue();
							int quantity = ((Long)attachmentData.get("quantity")).intValue();
							long price = (Long)attachmentData.get("price");
							transaction.attachment = new ColoredCoinsBidOrderPlacementAttachment(asset, quantity, price);
							
						}
						break;
						
					case SUBTYPE_COLORED_COINS_ASK_ORDER_CANCELLATION:
						{
							
							long order = (new BigInteger((String)attachmentData.get("order"))).longValue();
							transaction.attachment = new ColoredCoinsAskOrderCancellationAttachment(order);
							
						}
						break;
						
					case SUBTYPE_COLORED_COINS_BID_ORDER_CANCELLATION:
						{
							
							long order = (new BigInteger((String)attachmentData.get("order"))).longValue();
							transaction.attachment = new ColoredCoinsBidOrderCancellationAttachment(order);
							
						}
						break;
						
					}
					
				}
				break;
				
			}
			
			return transaction;
			
		}
		
		static void loadTransactions(String fileName) throws Exception {

            try (FileInputStream fileInputStream = new FileInputStream(fileName);
                 ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream)) {
                transactionCounter.set(objectInputStream.readInt());
                transactions.clear();
                transactions.putAll((HashMap<Long, Transaction>) objectInputStream.readObject());
            }

        }
		
		static void processTransactions(JSONObject request, String parameterName) {
			
			JSONArray transactionsData = (JSONArray)request.get(parameterName);
			JSONArray validTransactionsData = new JSONArray();
			
			for (Object transactionData : transactionsData) {
				
				Transaction transaction = Transaction.getTransaction((JSONObject)transactionData);
				
				try {
					
					int curTime = getEpochTime(System.currentTimeMillis());
					if (transaction.timestamp > curTime + 15 || transaction.deadline < 1 || transaction.timestamp + transaction.deadline * 60 < curTime || transaction.fee <= 0 || !transaction.validateAttachment()) {
						
						continue;
						
					}

                    long senderId;
                    boolean doubleSpendingTransaction;

					synchronized (blocksAndTransactionsLock) {
						
						long id = transaction.getId();
						if (transactions.get(id) != null || unconfirmedTransactions.get(id) != null || doubleSpendingTransactions.get(id) != null || !transaction.verify()) {
							
							continue;
							
						}
						
						senderId = Account.getId(transaction.senderPublicKey);
						Account account = accounts.get(senderId);
						if (account == null) {
							
							doubleSpendingTransaction = true;
							
						} else {
							
							int amount = transaction.amount + transaction.fee;
							synchronized (account) {
								
								if (account.unconfirmedBalance < amount * 100L) {
									
									doubleSpendingTransaction = true;
									
								} else {
									
									doubleSpendingTransaction = false;
									
									account.setUnconfirmedBalance(account.unconfirmedBalance - amount * 100L);
									
									if (transaction.type == Transaction.TYPE_COLORED_COINS) {
										
										if (transaction.subtype == Transaction.SUBTYPE_COLORED_COINS_ASSET_TRANSFER) {
											
											Transaction.ColoredCoinsAssetTransferAttachment attachment = (Transaction.ColoredCoinsAssetTransferAttachment)transaction.attachment;
											if (account.unconfirmedAssetBalances.get(attachment.asset) == null || account.unconfirmedAssetBalances.get(attachment.asset) < attachment.quantity) {
												
												doubleSpendingTransaction = true;
												
												account.setUnconfirmedBalance(account.unconfirmedBalance + amount * 100L);
												
											} else {
												
												account.unconfirmedAssetBalances.put(attachment.asset, account.unconfirmedAssetBalances.get(attachment.asset) - attachment.quantity);
												
											}
											
										} else if (transaction.subtype == Transaction.SUBTYPE_COLORED_COINS_ASK_ORDER_PLACEMENT) {
											
											Transaction.ColoredCoinsAskOrderPlacementAttachment attachment = (Transaction.ColoredCoinsAskOrderPlacementAttachment)transaction.attachment;
											if (account.unconfirmedAssetBalances.get(attachment.asset) == null || account.unconfirmedAssetBalances.get(attachment.asset) < attachment.quantity) {
												
												doubleSpendingTransaction = true;
												
												account.setUnconfirmedBalance(account.unconfirmedBalance + amount * 100L);
												
											} else {
												
												account.unconfirmedAssetBalances.put(attachment.asset, account.unconfirmedAssetBalances.get(attachment.asset) - attachment.quantity);
												
											}
											
										} else if (transaction.subtype == Transaction.SUBTYPE_COLORED_COINS_BID_ORDER_PLACEMENT) {
											
											Transaction.ColoredCoinsBidOrderPlacementAttachment attachment = (Transaction.ColoredCoinsBidOrderPlacementAttachment)transaction.attachment;
											if (account.unconfirmedBalance < attachment.quantity * attachment.price) {
												
												doubleSpendingTransaction = true;
												
												account.setUnconfirmedBalance(account.unconfirmedBalance + amount * 100L);
												
											} else {
												
												account.setUnconfirmedBalance(account.unconfirmedBalance - attachment.quantity * attachment.price);
												
											}
											
										}
										
									}
									
								}
								
							}
							
						}
						
						transaction.index = transactionCounter.incrementAndGet();
						
						if (doubleSpendingTransaction) {
							
							doubleSpendingTransactions.put(transaction.getId(), transaction);
							
						} else {
							
							unconfirmedTransactions.put(transaction.getId(), transaction);
							
							if (parameterName.equals("transactions")) {
								
								validTransactionsData.add(transactionData);
								
							}
							
						}

                    }

                    JSONObject response = new JSONObject();
                    response.put("response", "processNewData");

                    JSONArray newTransactions = new JSONArray();
                    JSONObject newTransaction = new JSONObject();
                    newTransaction.put("index", transaction.index);
                    newTransaction.put("timestamp", transaction.timestamp);
                    newTransaction.put("deadline", transaction.deadline);
                    newTransaction.put("recipient", convert(transaction.recipient));
                    newTransaction.put("amount", transaction.amount);
                    newTransaction.put("fee", transaction.fee);
                    newTransaction.put("sender", convert(senderId));
                    newTransaction.put("id", convert(transaction.getId()));
                    newTransactions.add(newTransaction);

                    if (doubleSpendingTransaction) {

                        response.put("addedDoubleSpendingTransactions", newTransactions);

                    } else {

                        response.put("addedUnconfirmedTransactions", newTransactions);

                    }

                    for (User user : users.values()) {

                        user.send(response);

                    }

				} catch (Exception e) {
					
					logMessage("15: " + e.toString());
					
				}
				
			}
			
			if (validTransactionsData.size() > 0) {
				
				JSONObject peerRequest = new JSONObject();
				peerRequest.put("requestType", "processTransactions");
				peerRequest.put("transactions", validTransactionsData);
				
				Peer.sendToAllPeers(peerRequest);
				
			}
			
		}
		
		static void saveTransactions(String fileName) throws Exception {

            try (FileOutputStream fileOutputStream = new FileOutputStream(fileName);
                 ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream)
            ) {
                objectOutputStream.writeInt(transactionCounter.get());
                objectOutputStream.writeObject(new HashMap(transactions));
                objectOutputStream.close();
            }

        }
		
		void sign(String secretPhrase) {
			
			signature = Crypto.sign(getBytes(), secretPhrase);
			
			try {
				
				while (!verify()) {
					
					timestamp++;
					signature = new byte[64];
					signature = Crypto.sign(getBytes(), secretPhrase);
					
				}
				
			} catch (Exception e) {
				
				logMessage("16: " + e.toString());
				
			}
			
		}
		
		boolean validateAttachment() {

			if (fee > MAX_BALANCE) {
				
				return false;
				
			}
			//TODO: refactor switch statements
			switch (type) {
			
			case TYPE_PAYMENT:
				{
					
					switch (subtype) {
					
					case SUBTYPE_PAYMENT_ORDINARY_PAYMENT:
						{
							
							if (amount <= 0 || amount > MAX_BALANCE) {
								
								return false;
								
							} else {
								
								return true;
								
							}
							
						}
						
					default:
						{
							
							return false;
							
						}
						
					}
					
				}
				
			case TYPE_MESSAGING:
				{
					
					switch (subtype) {
					
					case SUBTYPE_MESSAGING_ALIAS_ASSIGNMENT:
						{
							
							if (Block.getLastBlock().height < ALIAS_SYSTEM_BLOCK) {
								
								return false;
								
							}
							
							try {
								
								MessagingAliasAssignmentAttachment attachment = (MessagingAliasAssignmentAttachment)this.attachment;
								if (recipient != CREATOR_ID || amount != 0 || attachment.alias.length() == 0 || attachment.alias.length() > 100 || attachment.uri.length() > 1000) {
									
									return false;
									
								} else {
									
									String normalizedAlias = attachment.alias.toLowerCase();
									for (int i = 0; i < normalizedAlias.length(); i++) {
										
										if (alphabet.indexOf(normalizedAlias.charAt(i)) < 0) {
											
											return false;
											
										}
										
									}
									
									Alias alias = aliases.get(normalizedAlias);
									if (alias != null && alias.account.id != Account.getId(senderPublicKey)) {
										
										return false;
										
									}
									
									return true;
									
								}
								
							} catch (Exception e) {
								
								return false;
								
							}
							
						}
						
					default:
						{
							
							return false;
							
						}
						
					}
					
				}
				
			/*case TYPE_COLORED_COINS:
				{
					
					switch (subtype) {
					
					case SUBTYPE_COLORED_COINS_ASSET_ISSUANCE:
						{
							
							try {
								
								ColoredCoinsAssetIssuanceAttachment attachment = (ColoredCoinsAssetIssuanceAttachment)this.attachment;
								if (recipient != CREATOR_ID || amount != 0 || fee < ASSET_ISSUANCE_FEE || attachment.name.length() < 3 || attachment.name.length() > 10 || attachment.description.length() > 1000 || attachment.quantity <= 0 || attachment.quantity > 1000000000) {
									
									return false;
									
								} else {
									
									String normalizedName = attachment.name.toLowerCase();
									for (int i = 0; i < normalizedName.length(); i++) {
										
										if (alphabet.indexOf(normalizedName.charAt(i)) < 0) {
											
											return false;
											
										}
										
									}
									if (assetNameToIdMappings.get(normalizedName) != null) {
										
										return false;
										
									}
									
									return true;
									
								}
								
							} catch (Exception e) {
								
								return false;
								
							}
							
						}
						
					case SUBTYPE_COLORED_COINS_ASSET_TRANSFER:
						{
							
							ColoredCoinsAssetTransferAttachment attachment = (ColoredCoinsAssetTransferAttachment)this.attachment;
							if (amount != 0 || attachment.quantity <= 0 || attachment.quantity > 1000000000) {
								
								return false;
								
							} else {
								
								return true;
								
							}
							
						}
						
					case SUBTYPE_COLORED_COINS_ASK_ORDER_PLACEMENT:
						{
							
							ColoredCoinsAskOrderPlacementAttachment attachment = (ColoredCoinsAskOrderPlacementAttachment)this.attachment;
							if (recipient != CREATOR_ID || amount != 0 || attachment.quantity <= 0 || attachment.quantity > 1000000000 || attachment.price <= 0 || attachment.price > 1000000000 * 100L) {
								
								return false;
								
							} else {
								
								return true;
								
							}
							
						}
						
					case SUBTYPE_COLORED_COINS_BID_ORDER_PLACEMENT:
						{
							
							ColoredCoinsBidOrderPlacementAttachment attachment = (ColoredCoinsBidOrderPlacementAttachment)this.attachment;
							if (recipient != CREATOR_ID || amount != 0 || attachment.quantity <= 0 || attachment.quantity > 1000000000 || attachment.price <= 0 || attachment.price > 1000000000 * 100L) {
								
								return false;
								
							} else {
								
								return true;
								
							}
							
						}
						
					case SUBTYPE_COLORED_COINS_ASK_ORDER_CANCELLATION:
						{
							
							if (recipient != CREATOR_ID || amount != 0) {
								
								return false;
								
							} else {
								
								return true;
								
							}
							
						}
						
					case SUBTYPE_COLORED_COINS_BID_ORDER_CANCELLATION:
						{
							
							if (recipient != CREATOR_ID || amount != 0) {
								
								return false;
								
							} else {
								
								return true;
								
							}
							
						}
						
					default:
						{
							
							return false;
							
						}
						
					}
					
				}*/
				
			default:
				{
					
					return false;
					
				}
				
			}
			
		}
		
		boolean verify() throws Exception {
			
			Account account = accounts.get(Account.getId(senderPublicKey));
			if (account == null) {
				
				return false;
				
			} else if (account.publicKey == null) {
				
				account.publicKey = senderPublicKey;
				
			} else if (!Arrays.equals(senderPublicKey, account.publicKey)) {
				
				return false;
				
			}
			
			byte[] data = getBytes();
			for (int i = 64; i < 128; i++) {
				
				data[i] = 0;
				
			}
			
			return Crypto.verify(signature, data, senderPublicKey);
			
		}
		
		static interface Attachment {
			
			byte[] getBytes();
			JSONObject getJSONObject();
			
		}
		
		static class MessagingAliasAssignmentAttachment implements Attachment, Serializable {
			
			static final long serialVersionUID = 0;

            final String alias;
			final String uri;
			
			MessagingAliasAssignmentAttachment(String alias, String uri) {
				
				this.alias = alias;
				this.uri = uri;
				
			}

            //TODO: cache?
			@Override
			public byte[] getBytes() {
				
				try {
					
					byte[] alias = this.alias.getBytes("UTF-8");
					byte[] uri = this.uri.getBytes("UTF-8");
					
					ByteBuffer buffer = ByteBuffer.allocate(1 + alias.length + 2 + uri.length);
					buffer.order(ByteOrder.LITTLE_ENDIAN);
					buffer.put((byte)alias.length);
					buffer.put(alias);
					buffer.putShort((short)uri.length);
					buffer.put(uri);
					
					return buffer.array();
					
				} catch (Exception e) {
					
					return null;
					
				}
				
			}
			
			@Override
			public JSONObject getJSONObject() {
				
				JSONObject attachment = new JSONObject();
				attachment.put("alias", alias);
				attachment.put("uri", uri);
				
				return attachment;
				
			}
			
		}
		
		static class ColoredCoinsAssetIssuanceAttachment implements Attachment, Serializable {
			
			static final long serialVersionUID = 0;
			
			String name;
			String description;
			int quantity;
			
			ColoredCoinsAssetIssuanceAttachment(String name, String description, int quantity) {
				
				this.name = name;
				this.description = description == null ? "" : description;
				this.quantity = quantity;
				
			}
			
			@Override
			public byte[] getBytes() {
				
				try {
					
					byte[] name = this.name.getBytes("UTF-8");
					byte[] description = this.description.getBytes("UTF-8");
					
					ByteBuffer buffer = ByteBuffer.allocate(1 + name.length + 2 + description.length + 4);
					buffer.order(ByteOrder.LITTLE_ENDIAN);
					buffer.put((byte)name.length);
					buffer.put(name);
					buffer.putShort((short)description.length);
					buffer.put(description);
					buffer.putInt(quantity);
					
					return buffer.array();
					
				} catch (Exception e) {
					
					return null;
					
				}
				
			}
			
			@Override
			public JSONObject getJSONObject() {
				
				JSONObject attachment = new JSONObject();
				attachment.put("name", name);
				attachment.put("description", description);
				attachment.put("quantity", quantity);
				
				return attachment;
				
			}
			
		}
		
		static class ColoredCoinsAssetTransferAttachment implements Attachment, Serializable {
			
			static final long serialVersionUID = 0;
			
			long asset;
			int quantity;
			
			ColoredCoinsAssetTransferAttachment(long asset, int quantity) {
				
				this.asset = asset;
				this.quantity = quantity;
				
			}
			
			@Override
			public byte[] getBytes() {
				
				ByteBuffer buffer = ByteBuffer.allocate(8 + 4);
				buffer.order(ByteOrder.LITTLE_ENDIAN);
				buffer.putLong(asset);
				buffer.putInt(quantity);
				
				return buffer.array();
				
			}
			
			@Override
			public JSONObject getJSONObject() {
				
				JSONObject attachment = new JSONObject();
				attachment.put("asset", convert(asset));
				attachment.put("quantity", quantity);
				
				return attachment;
				
			}
			
		}
		
		static class ColoredCoinsAskOrderPlacementAttachment implements Attachment, Serializable {
			
			static final long serialVersionUID = 0;
			
			long asset;
			int quantity;
			long price;
			
			ColoredCoinsAskOrderPlacementAttachment(long asset, int quantity, long price) {
				
				this.asset = asset;
				this.quantity = quantity;
				this.price = price;
				
			}
			
			@Override
			public byte[] getBytes() {
				
				ByteBuffer buffer = ByteBuffer.allocate(8 + 4 + 8);
				buffer.order(ByteOrder.LITTLE_ENDIAN);
				buffer.putLong(asset);
				buffer.putInt(quantity);
				buffer.putLong(price);
				
				return buffer.array();
				
			}
			
			@Override
			public JSONObject getJSONObject() {
				
				JSONObject attachment = new JSONObject();
				attachment.put("asset", convert(asset));
				attachment.put("quantity", quantity);
				attachment.put("price", price);
				
				return attachment;
				
			}
			
		}
		
		static class ColoredCoinsBidOrderPlacementAttachment implements Attachment, Serializable {
			
			static final long serialVersionUID = 0;
			
			long asset;
			int quantity;
			long price;
			
			ColoredCoinsBidOrderPlacementAttachment(long asset, int quantity, long price) {
				
				this.asset = asset;
				this.quantity = quantity;
				this.price = price;
				
			}
			
			@Override
			public byte[] getBytes() {
				
				ByteBuffer buffer = ByteBuffer.allocate(8 + 4 + 8);
				buffer.order(ByteOrder.LITTLE_ENDIAN);
				buffer.putLong(asset);
				buffer.putInt(quantity);
				buffer.putLong(price);
				
				return buffer.array();
				
			}
			
			@Override
			public JSONObject getJSONObject() {
				
				JSONObject attachment = new JSONObject();
				attachment.put("asset", convert(asset));
				attachment.put("quantity", quantity);
				attachment.put("price", price);
				
				return attachment;
				
			}
			
		}
		
		static class ColoredCoinsAskOrderCancellationAttachment implements Attachment, Serializable {
			
			static final long serialVersionUID = 0;
			
			long order;
			
			ColoredCoinsAskOrderCancellationAttachment(long order) {
				
				this.order = order;
				
			}
			
			@Override
			public byte[] getBytes() {
				
				ByteBuffer buffer = ByteBuffer.allocate(8);
				buffer.order(ByteOrder.LITTLE_ENDIAN);
				buffer.putLong(order);
				
				return buffer.array();
				
			}
			
			@Override
			public JSONObject getJSONObject() {
				
				JSONObject attachment = new JSONObject();
				attachment.put("order", convert(order));
				
				return attachment;
				
			}
			
		}
		
		static class ColoredCoinsBidOrderCancellationAttachment implements Attachment, Serializable {
			
			static final long serialVersionUID = 0;
			
			long order;
			
			ColoredCoinsBidOrderCancellationAttachment(long order) {
				
				this.order = order;
				
			}
			
			@Override
			public byte[] getBytes() {
				
				ByteBuffer buffer = ByteBuffer.allocate(8);
				buffer.order(ByteOrder.LITTLE_ENDIAN);
				buffer.putLong(order);
				
				return buffer.array();
				
			}
			
			@Override
			public JSONObject getJSONObject() {
				
				JSONObject attachment = new JSONObject();
				attachment.put("order", convert(order));
				
				return attachment;
				
			}
			
		}
		
	}
	
	static class User {
		
		final ConcurrentLinkedQueue<JSONObject> pendingResponses;
        //TODO: asyncContext access not thread safe
        AsyncContext asyncContext;
		volatile boolean isInactive;
		
		String secretPhrase;
		
		User() {
			
			pendingResponses = new ConcurrentLinkedQueue<>();
			
		}
		
		void deinitializeKeyPair() {
			
			secretPhrase = null;
			
		}
		
		BigInteger initializeKeyPair(String secretPhrase) throws Exception {
			
			this.secretPhrase = secretPhrase;
			byte[] publicKeyHash = MessageDigest.getInstance("SHA-256").digest(Crypto.getPublicKey(secretPhrase));
			BigInteger bigInteger = new BigInteger(1, new byte[] {publicKeyHash[7], publicKeyHash[6], publicKeyHash[5], publicKeyHash[4], publicKeyHash[3], publicKeyHash[2], publicKeyHash[1], publicKeyHash[0]});
			
			return bigInteger;
			
		}
		
		void send(JSONObject response) {
			
			synchronized (this) {
				
				if (asyncContext == null) {
					
                    if (isInactive) {
                        // user not seen recently, no responses should be collected
                        return;
                    }
                    if (pendingResponses.size() > 1000) {
                        pendingResponses.clear();
                        // stop collecting responses for this user
                        isInactive = true;
                        if (secretPhrase == null) {
                            // but only completely remove users that don't have unlocked accounts
                            Nxt.users.values().remove(this);
                        }
                        return;
                    }
                    
					pendingResponses.offer(response);
					
				} else {
					
					JSONArray responses = new JSONArray();
					JSONObject pendingResponse;
					while ((pendingResponse = pendingResponses.poll()) != null) {
						
						responses.add(pendingResponse);
						
					}
					responses.add(response);
					
					JSONObject combinedResponse = new JSONObject();
					combinedResponse.put("responses", responses);
					
					try {
						
						asyncContext.getResponse().setContentType("text/plain; charset=UTF-8");
						
						try (ServletOutputStream servletOutputStream = asyncContext.getResponse().getOutputStream()) {
						    servletOutputStream.write(combinedResponse.toString().getBytes("UTF-8"));
                        }

						asyncContext.complete();
						asyncContext = null;
						
					} catch (Exception e) {
						
						logMessage("17: " + e.toString());
						
					}
					
				}
				
			}
			
		}
		
	}
	
	static class UserAsyncListener implements AsyncListener {
		
		User user;
		
		UserAsyncListener(User user) {
			
			this.user = user;
			
		}
		
		@Override
		public void onComplete(AsyncEvent asyncEvent) throws IOException { }
		
		@Override
		public void onError(AsyncEvent asyncEvent) throws IOException {

            user.asyncContext.getResponse().setContentType("text/plain; charset=UTF-8");
			
			try (ServletOutputStream servletOutputStream = user.asyncContext.getResponse().getOutputStream()) {
    			servletOutputStream.write((new JSONObject()).toString().getBytes("UTF-8"));
            }

			user.asyncContext.complete();
			user.asyncContext = null;
			
		}
		
		@Override
		public void onStartAsync(AsyncEvent asyncEvent) throws IOException { }
		
		@Override
		public void onTimeout(AsyncEvent asyncEvent) throws IOException {
			
			user.asyncContext.getResponse().setContentType("text/plain; charset=UTF-8");
			
			try (ServletOutputStream servletOutputStream = user.asyncContext.getResponse().getOutputStream()) {
    			servletOutputStream.write((new JSONObject()).toString().getBytes("UTF-8"));
            }

			user.asyncContext.complete();
			user.asyncContext = null;
			
		}
		
	}
	
	@Override
	public void init(ServletConfig servletConfig) throws ServletException {
		
		logMessage("Nxt " + VERSION + " started.");
		
		try {
			
			Calendar calendar = Calendar.getInstance();
			calendar.set(Calendar.ZONE_OFFSET, 0);
			calendar.set(Calendar.YEAR, 2013);
			calendar.set(Calendar.MONTH, Calendar.NOVEMBER);
			calendar.set(Calendar.DAY_OF_MONTH, 24);
			calendar.set(Calendar.HOUR_OF_DAY, 12);
			calendar.set(Calendar.MINUTE, 0);
			calendar.set(Calendar.SECOND, 0);
			calendar.set(Calendar.MILLISECOND, 0);
			epochBeginning = calendar.getTimeInMillis();
			
			String blockchainStoragePath = servletConfig.getInitParameter("blockchainStoragePath");
			logMessage("\"blockchainStoragePath\" = \"" + blockchainStoragePath + "\"");
			//blockchainChannel = FileChannel.open(Paths.get(blockchainStoragePath), StandardOpenOption.READ, StandardOpenOption.WRITE);
			
			myPlatform = servletConfig.getInitParameter("myPlatform");
			logMessage("\"myPlatform\" = \"" + myPlatform + "\"");
			if (myPlatform == null) {
				
				myPlatform = "PC";
				
			} else {
				
				myPlatform = myPlatform.trim();
				
			}
			
			myScheme = servletConfig.getInitParameter("myScheme");
			logMessage("\"myScheme\" = \"" + myScheme + "\"");
			if (myScheme == null) {
				
				myScheme = "http";
				
			} else {
				
				myScheme = myScheme.trim();
				
			}
			
			String myPort = servletConfig.getInitParameter("myPort");
			logMessage("\"myPort\" = \"" + myPort + "\"");
			try {
				
				Nxt.myPort = Integer.parseInt(myPort);
				
			} catch (Exception e) {
				
				Nxt.myPort = myScheme.equals("https") ? 7875 : 7874;
				
			}
			
			myAddress = servletConfig.getInitParameter("myAddress");
			logMessage("\"myAddress\" = \"" + myAddress + "\"");
			if (myAddress != null) {
				
				myAddress = myAddress.trim();
				
			}
			
			String shareMyAddress = servletConfig.getInitParameter("shareMyAddress");
			logMessage("\"shareMyAddress\" = \"" + shareMyAddress + "\"");
			try {
				
				Nxt.shareMyAddress = Boolean.parseBoolean(shareMyAddress);
				
			} catch (Exception e) {
				
				Nxt.shareMyAddress = true;
				
			}
			
			myHallmark = servletConfig.getInitParameter("myHallmark");
			logMessage("\"myHallmark\" = \"" + myHallmark + "\"");
			if (myHallmark != null) {
				
				myHallmark = myHallmark.trim();
				
			}
			
			String wellKnownPeers = servletConfig.getInitParameter("wellKnownPeers");
			logMessage("\"wellKnownPeers\" = \"" + wellKnownPeers + "\"");
			if (wellKnownPeers != null) {
                Set<String> set = new HashSet<>();
				for (String wellKnownPeer : wellKnownPeers.split(";")) {
					
					wellKnownPeer = wellKnownPeer.trim();
					if (wellKnownPeer.length() > 0) {
						
						set.add(wellKnownPeer);
						Peer.addPeer(wellKnownPeer, wellKnownPeer);
						
					}
					
				}
                Nxt.wellKnownPeers = Collections.unmodifiableSet(set);
            } else {
                Nxt.wellKnownPeers = Collections.emptySet();
                logMessage("No wellKnownPeers defined, it is unlikely to work");
            }

			String maxNumberOfConnectedPublicPeers = servletConfig.getInitParameter("maxNumberOfConnectedPublicPeers");
			logMessage("\"maxNumberOfConnectedPublicPeers\" = \"" + maxNumberOfConnectedPublicPeers + "\"");
			try {
				
				Nxt.maxNumberOfConnectedPublicPeers = Integer.parseInt(maxNumberOfConnectedPublicPeers);
				
			} catch (Exception e) {
				
				Nxt.maxNumberOfConnectedPublicPeers = 10;
				
			}
			
			String connectTimeout = servletConfig.getInitParameter("connectTimeout");
			logMessage("\"connectTimeout\" = \"" + connectTimeout + "\"");
			try {
				
				Nxt.connectTimeout = Integer.parseInt(connectTimeout);
				
			} catch (Exception e) {
				
				Nxt.connectTimeout = 1000;
				
			}
			
			String readTimeout = servletConfig.getInitParameter("readTimeout");
			logMessage("\"readTimeout\" = \"" + readTimeout + "\"");
			try {
				
				Nxt.readTimeout = Integer.parseInt(readTimeout);
				
			} catch (Exception e) {
				
				Nxt.readTimeout = 1000;
				
			}
			
			String enableHallmarkProtection = servletConfig.getInitParameter("enableHallmarkProtection");
			logMessage("\"enableHallmarkProtection\" = \"" + enableHallmarkProtection + "\"");
			try {
				
				Nxt.enableHallmarkProtection = Boolean.parseBoolean(enableHallmarkProtection);
				
			} catch (Exception e) {
				
				Nxt.enableHallmarkProtection = true;
				
			}
			
			String pushThreshold = servletConfig.getInitParameter("pushThreshold");
			logMessage("\"pushThreshold\" = \"" + pushThreshold + "\"");
			try {
				
				Nxt.pushThreshold = Integer.parseInt(pushThreshold);
				
			} catch (Exception e) {
				
				Nxt.pushThreshold = 0;
				
			}
			
			String pullThreshold = servletConfig.getInitParameter("pullThreshold");
			logMessage("\"pullThreshold\" = \"" + pullThreshold + "\"");
			try {
				
				Nxt.pullThreshold = Integer.parseInt(pullThreshold);
				
			} catch (Exception e) {
				
				Nxt.pullThreshold = 0;
				
			}
			
			String allowedUserHosts = servletConfig.getInitParameter("allowedUserHosts");
			logMessage("\"allowedUserHosts\" = \"" + allowedUserHosts + "\"");
			if (allowedUserHosts != null) {
				
				if (!allowedUserHosts.trim().equals("*")) {
					
					Set<String> set = new HashSet<>();
					for (String allowedUserHost : allowedUserHosts.split(";")) {
						
						allowedUserHost = allowedUserHost.trim();
						if (allowedUserHost.length() > 0) {
							
							set.add(allowedUserHost);
							
						}
						
					}
                    Nxt.allowedUserHosts = Collections.unmodifiableSet(set);

                }
				
			}
			
			String allowedBotHosts = servletConfig.getInitParameter("allowedBotHosts");
			logMessage("\"allowedBotHosts\" = \"" + allowedBotHosts + "\"");
			if (allowedBotHosts != null) {
				
				if (!allowedBotHosts.trim().equals("*")) {
					
					Set<String> set = new HashSet<>();
					for (String allowedBotHost : allowedBotHosts.split(";")) {
						
						allowedBotHost = allowedBotHost.trim();
						if (allowedBotHost.length() > 0) {
							
							set.add(allowedBotHost);
							
						}
						
					}
					Nxt.allowedBotHosts = Collections.unmodifiableSet(set);
				}
				
			}
			
			String blacklistingPeriod = servletConfig.getInitParameter("blacklistingPeriod");
			logMessage("\"blacklistingPeriod\" = \"" + blacklistingPeriod + "\"");
			try {
				
				Nxt.blacklistingPeriod = Integer.parseInt(blacklistingPeriod);
				
			} catch (Exception e) {
				
				Nxt.blacklistingPeriod = 300000;
				
			}
			
			String communicationLoggingMask = servletConfig.getInitParameter("communicationLoggingMask");
			logMessage("\"communicationLoggingMask\" = \"" + communicationLoggingMask + "\"");
			try {
				
				Nxt.communicationLoggingMask = Integer.parseInt(communicationLoggingMask);
				
			} catch (Exception e) { }
			
			try {
				
				logMessage("Loading transactions...");
				Transaction.loadTransactions("transactions.nxt");
				logMessage("...Done");
				
			} catch (FileNotFoundException e) {	

                transactions.clear();

				long[] recipients = {(new BigInteger("163918645372308887")).longValue(),
						(new BigInteger("620741658595224146")).longValue(),
						(new BigInteger("723492359641172834")).longValue(),
						(new BigInteger("818877006463198736")).longValue(),
						(new BigInteger("1264744488939798088")).longValue(),
						(new BigInteger("1600633904360147460")).longValue(),
						(new BigInteger("1796652256451468602")).longValue(),
						(new BigInteger("1814189588814307776")).longValue(),
						(new BigInteger("1965151371996418680")).longValue(),
						(new BigInteger("2175830371415049383")).longValue(),
						(new BigInteger("2401730748874927467")).longValue(),
						(new BigInteger("2584657662098653454")).longValue(),
						(new BigInteger("2694765945307858403")).longValue(),
						(new BigInteger("3143507805486077020")).longValue(),
						(new BigInteger("3684449848581573439")).longValue(),
						(new BigInteger("4071545868996394636")).longValue(),
						(new BigInteger("4277298711855908797")).longValue(),
						(new BigInteger("4454381633636789149")).longValue(),
						(new BigInteger("4747512364439223888")).longValue(),
						(new BigInteger("4777958973882919649")).longValue(),
						(new BigInteger("4803826772380379922")).longValue(),
						(new BigInteger("5095742298090230979")).longValue(),
						(new BigInteger("5271441507933314159")).longValue(),
						(new BigInteger("5430757907205901788")).longValue(),
						(new BigInteger("5491587494620055787")).longValue(),
						(new BigInteger("5622658764175897611")).longValue(),
						(new BigInteger("5982846390354787993")).longValue(),
						(new BigInteger("6290807999797358345")).longValue(),
						(new BigInteger("6785084810899231190")).longValue(),
						(new BigInteger("6878906112724074600")).longValue(),
						(new BigInteger("7017504655955743955")).longValue(),
						(new BigInteger("7085298282228890923")).longValue(),
						(new BigInteger("7446331133773682477")).longValue(),
						(new BigInteger("7542917420413518667")).longValue(),
						(new BigInteger("7549995577397145669")).longValue(),
						(new BigInteger("7577840883495855927")).longValue(),
						(new BigInteger("7579216551136708118")).longValue(),
						(new BigInteger("8278234497743900807")).longValue(),
						(new BigInteger("8517842408878875334")).longValue(),
						(new BigInteger("8870453786186409991")).longValue(),
						(new BigInteger("9037328626462718729")).longValue(),
						(new BigInteger("9161949457233564608")).longValue(),
						(new BigInteger("9230759115816986914")).longValue(),
						(new BigInteger("9306550122583806885")).longValue(),
						(new BigInteger("9433259657262176905")).longValue(),
						(new BigInteger("9988839211066715803")).longValue(),
						(new BigInteger("10105875265190846103")).longValue(),
						(new BigInteger("10339765764359265796")).longValue(),
						(new BigInteger("10738613957974090819")).longValue(),
						(new BigInteger("10890046632913063215")).longValue(),
						(new BigInteger("11494237785755831723")).longValue(),
						(new BigInteger("11541844302056663007")).longValue(),
						(new BigInteger("11706312660844961581")).longValue(),
						(new BigInteger("12101431510634235443")).longValue(),
						(new BigInteger("12186190861869148835")).longValue(),
						(new BigInteger("12558748907112364526")).longValue(),
						(new BigInteger("13138516747685979557")).longValue(),
						(new BigInteger("13330279748251018740")).longValue(),
						(new BigInteger("14274119416917666908")).longValue(),
						(new BigInteger("14557384677985343260")).longValue(),
						(new BigInteger("14748294830376619968")).longValue(),
						(new BigInteger("14839596582718854826")).longValue(),
						(new BigInteger("15190676494543480574")).longValue(),
						(new BigInteger("15253761794338766759")).longValue(),
						(new BigInteger("15558257163011348529")).longValue(),
						(new BigInteger("15874940801139996458")).longValue(),
						(new BigInteger("16516270647696160902")).longValue(),
						(new BigInteger("17156841960446798306")).longValue(),
						(new BigInteger("17228894143802851995")).longValue(),
						(new BigInteger("17240396975291969151")).longValue(),
						(new BigInteger("17491178046969559641")).longValue(),
						(new BigInteger("18345202375028346230")).longValue(),
						(new BigInteger("18388669820699395594")).longValue()};
				int[] amounts = {36742,
						1970092,
						349130,
						24880020,
						2867856,
						9975150,
						2690963,
						7648,
						5486333,
						34913026,
						997515,
						30922966,
						6650,
						44888,
						2468850,
						49875751,
						49875751,
						9476393,
						49875751,
						14887912,
						528683,
						583546,
						7315,
						19925363,
						29856290,
						5320,
						4987575,
						5985,
						24912938,
						49875751,
						2724712,
						1482474,
						200999,
						1476156,
						498758,
						987540,
						16625250,
						5264386,
						15487585,
						2684479,
						14962725,
						34913026,
						5033128,
						2916900,
						49875751,
						4962637,
						170486123,
						8644631,
						22166945,
						6668388,
						233751,
						4987575,
						11083556,
						1845403,
						49876,
						3491,
						3491,
						9476,
						49876,
						6151,
						682633,
						49875751,
						482964,
						4988,
						49875751,
						4988,
						9144,
						503745,
						49875751,
						52370,
						29437998,
						585375,
						9975150};
				byte[][] signatures = {{41, 115, -41, 7, 37, 21, -3, -41, 120, 119, 63, -101, 108, 48, -117, 1, -43, 32, 85, 95, 65, 42, 92, -22, 123, -36, 6, -99, -61, -53, 93, 7, 23, 8, -30, 65, 57, -127, -2, 42, -92, -104, 11, 72, -66, 108, 17, 113, 99, -117, -75, 123, 110, 107, 119, -25, 67, 64, 32, 117, 111, 54, 82, -14},
						{118, 43, 84, -91, -110, -102, 100, -40, -33, -47, -13, -7, -88, 2, -42, -66, -38, -22, 105, -42, -69, 78, 51, -55, -48, 49, -89, 116, -96, -104, -114, 14, 94, 58, -115, -8, 111, -44, 76, -104, 54, -15, 126, 31, 6, -80, 65, 6, 124, 37, -73, 92, 4, 122, 122, -108, 1, -54, 31, -38, -117, -1, -52, -56},
						{79, 100, -101, 107, -6, -61, 40, 32, -98, 32, 80, -59, -76, -23, -62, 38, 4, 105, -106, -105, -121, -85, 13, -98, -77, 126, -125, 103, 12, -41, 1, 2, 45, -62, -69, 102, 116, -61, 101, -14, -68, -31, 9, 110, 18, 2, 33, -98, -37, -128, 17, -19, 124, 125, -63, 92, -70, 96, 96, 125, 91, 8, -65, -12},
						{58, -99, 14, -97, -75, -10, 110, -102, 119, -3, -2, -12, -82, -33, -27, 118, -19, 55, -109, 6, 110, -10, 108, 30, 94, -113, -5, -98, 19, 12, -125, 14, -77, 33, -128, -21, 36, -120, -12, -81, 64, 95, 67, -3, 100, 122, -47, 127, -92, 114, 68, 72, 2, -40, 80, 117, -17, -56, 103, 37, -119, 3, 22, 23},
						{76, 22, 121, -4, -77, -127, 18, -102, 7, 94, -73, -96, 108, -11, 81, -18, -37, -85, -75, 86, -119, 94, 126, 95, 47, -36, -16, -50, -9, 95, 60, 15, 14, 93, -108, -83, -67, 29, 2, -53, 10, -118, -51, -46, 92, -23, -56, 60, 46, -90, -84, 126, 60, 78, 12, 53, 61, 121, -6, 77, 112, 60, 40, 63},
						{64, 121, -73, 68, 4, -103, 81, 55, -41, -81, -63, 10, 117, -74, 54, -13, -85, 79, 21, 116, -25, -12, 21, 120, -36, -80, 53, -78, 103, 25, 55, 6, -75, 96, 80, -125, -11, -103, -20, -41, 121, -61, -40, 63, 24, -81, -125, 90, -12, -40, -52, -1, -114, 14, -44, -112, -80, 83, -63, 88, -107, -10, -114, -86},
						{-81, 126, -41, -34, 66, -114, -114, 114, 39, 32, -125, -19, -95, -50, -111, -51, -33, 51, 99, -127, 58, 50, -110, 44, 80, -94, -96, 68, -69, 34, 86, 3, -82, -69, 28, 20, -111, 69, 18, -41, -23, 27, -118, 20, 72, 21, -112, 53, -87, -81, -47, -101, 123, -80, 99, -15, 33, -120, -8, 82, 80, -8, -10, -45},
						{92, 77, 53, -87, 26, 13, -121, -39, -62, -42, 47, 4, 7, 108, -15, 112, 103, 38, -50, -74, 60, 56, -63, 43, -116, 49, -106, 69, 118, 65, 17, 12, 31, 127, -94, 55, -117, -29, -117, 31, -95, -110, -2, 63, -73, -106, -88, -41, -19, 69, 60, -17, -16, 61, 32, -23, 88, -106, -96, 37, -96, 114, -19, -99},
						{68, -26, 57, -56, -30, 108, 61, 24, 106, -56, -92, 99, -59, 107, 25, -110, -57, 80, 79, -92, -107, 90, 54, -73, -40, -39, 78, 109, -57, -62, -17, 6, -25, -29, 37, 90, -24, -27, -61, -69, 44, 121, 107, -72, -57, 108, 32, -69, -21, -41, 126, 91, 118, 11, -91, 50, -11, 116, 126, -96, -39, 110, 105, -52},
						{48, 108, 123, 50, -50, -58, 33, 14, 59, 102, 17, -18, -119, 4, 10, -29, 36, -56, -31, 43, -71, -48, -14, 87, 119, -119, 40, 104, -44, -76, -24, 2, 48, -96, -7, 16, -119, -3, 108, 78, 125, 88, 61, -53, -3, -16, 20, -83, 74, 124, -47, -17, -15, -21, -23, -119, -47, 105, -4, 115, -20, 77, 57, 88},
						{33, 101, 79, -35, 32, -119, 20, 120, 34, -80, -41, 90, -22, 93, -20, -45, 9, 24, -46, 80, -55, -9, -24, -78, -124, 27, -120, -36, -51, 59, -38, 7, 113, 125, 68, 109, 24, -121, 111, 37, -71, 100, -111, 78, -43, -14, -76, -44, 64, 103, 16, -28, -44, -103, 74, 81, -118, -74, 47, -77, -65, 8, 42, -100},
						{-63, -96, -95, -111, -85, -98, -85, 42, 87, 29, -62, -57, 57, 48, 9, -39, -110, 63, -103, -114, -48, -11, -92, 105, -26, -79, -11, 78, -118, 14, -39, 1, -115, 74, 70, -41, -119, -68, -39, -60, 64, 31, 25, -111, -16, -20, 61, -22, 17, -13, 57, -110, 24, 61, -104, 21, -72, -69, 56, 116, -117, 93, -1, -123},
						{-18, -70, 12, 112, -111, 10, 22, 31, -120, 26, 53, 14, 10, 69, 51, 45, -50, -127, -22, 95, 54, 17, -8, 54, -115, 36, -79, 12, -79, 82, 4, 1, 92, 59, 23, -13, -85, -87, -110, -58, 84, -31, -48, -105, -101, -92, -9, 28, -109, 77, -47, 100, -48, -83, 106, -102, 70, -95, 94, -1, -99, -15, 21, 99},
						{109, 123, 54, 40, -120, 32, -118, 49, -52, 0, -103, 103, 101, -9, 32, 78, 124, -56, 88, -19, 101, -32, 70, 67, -41, 85, -103, 1, 1, -105, -51, 10, 4, 51, -26, -19, 39, -43, 63, -41, -101, 80, 106, -66, 125, 47, -117, -120, -93, -120, 99, -113, -17, 61, 102, -2, 72, 9, -124, 123, -128, 78, 43, 96},
						{-22, -63, 20, 65, 5, -89, -123, -61, 14, 34, 83, -113, 34, 85, 26, -21, 1, 16, 88, 55, -92, -111, 14, -31, -37, -67, -8, 85, 39, -112, -33, 8, 28, 16, 107, -29, 1, 3, 100, -53, 2, 81, 52, -94, -14, 36, -123, -82, -6, -118, 104, 75, -99, -82, -100, 7, 30, -66, 0, -59, 108, -54, 31, 20},
						{0, 13, -74, 28, -54, -12, 45, 36, -24, 55, 43, -110, -72, 117, -110, -56, -72, 85, 79, -89, -92, 65, -67, -34, -24, 38, 67, 42, 84, -94, 91, 13, 100, 89, 20, -95, -76, 2, 116, 34, 67, 52, -80, -101, -22, -32, 51, 32, -76, 44, -93, 11, 42, -69, -12, 7, -52, -55, 122, -10, 48, 21, 92, 110},
						{-115, 19, 115, 28, -56, 118, 111, 26, 18, 123, 111, -96, -115, 120, 105, 62, -123, -124, 101, 51, 3, 18, -89, 127, 48, -27, 39, -78, -118, 5, -2, 6, -105, 17, 123, 26, 25, -62, -37, 49, 117, 3, 10, 97, -7, 54, 121, -90, -51, -49, 11, 104, -66, 11, -6, 57, 5, -64, -8, 59, 82, -126, 26, -113},
						{16, -53, 94, 99, -46, -29, 64, -89, -59, 116, -21, 53, 14, -77, -71, 95, 22, -121, -51, 125, -14, -96, 95, 95, 32, 96, 79, 41, -39, -128, 79, 0, 5, 6, -115, 104, 103, 77, -92, 93, -109, 58, 96, 97, -22, 116, -62, 11, 30, -122, 14, 28, 69, 124, 63, -119, 19, 80, -36, -116, -76, -58, 36, 87},
						{109, -82, 33, -119, 17, 109, -109, -16, 98, 108, 111, 5, 98, 1, -15, -32, 22, 46, -65, 117, -78, 119, 35, -35, -3, 41, 23, -97, 55, 69, 58, 9, 20, -113, -121, -13, -41, -48, 22, -73, -1, -44, -73, 3, -10, -122, 19, -103, 10, -26, -128, 62, 34, 55, 54, -43, 35, -30, 115, 64, -80, -20, -25, 67},
						{-16, -74, -116, -128, 52, 96, -75, 17, -22, 72, -43, 22, -95, -16, 32, -72, 98, 46, -4, 83, 34, -58, -108, 18, 17, -58, -123, 53, -108, 116, 18, 2, 7, -94, -126, -45, 72, -69, -65, -89, 64, 31, -78, 78, -115, 106, 67, 55, -123, 104, -128, 36, -23, -90, -14, -87, 78, 19, 18, -128, 39, 73, 35, 120},
						{20, -30, 15, 111, -82, 39, -108, 57, -80, 98, -19, -27, 100, -18, 47, 77, -41, 95, 80, -113, -128, -88, -76, 115, 65, -53, 83, 115, 7, 2, -104, 3, 120, 115, 14, -116, 33, -15, -120, 22, -56, -8, -69, 5, -75, 94, 124, 12, -126, -48, 51, -105, 22, -66, -93, 16, -63, -74, 32, 114, -54, -3, -47, -126},
						{56, -101, 55, -1, 64, 4, -64, 95, 31, -15, 72, 46, 67, -9, 68, -43, -55, 28, -63, -17, -16, 65, 11, -91, -91, 32, 88, 41, 60, 67, 105, 8, 58, 102, -79, -5, -113, -113, -67, 82, 50, -26, 116, -78, -103, 107, 102, 23, -74, -47, 115, -50, -35, 63, -80, -32, 72, 117, 47, 68, 86, -20, -35, 8},
						{21, 27, 20, -59, 117, -102, -42, 22, -10, 121, 41, -59, 115, 15, -43, 54, -79, -62, -16, 58, 116, 15, 88, 108, 114, 67, 3, -30, -99, 78, 103, 11, 49, 63, -4, -110, -27, 41, 70, -57, -69, -18, 70, 30, -21, 66, -104, -27, 3, 53, 50, 100, -33, 54, -3, -78, 92, 85, -78, 54, 19, 32, 95, 9},
						{-93, 65, -64, -79, 82, 85, -34, -90, 122, -29, -40, 3, -80, -40, 32, 26, 102, -73, 17, 53, -93, -29, 122, 86, 107, -100, 50, 56, -28, 124, 90, 14, 93, -88, 97, 101, -85, -50, 46, -109, -88, -127, -112, 63, -89, 24, -34, -9, -116, -59, -87, -86, -12, 111, -111, 87, -87, -13, -73, -124, -47, 7, 1, 9},
						{60, -99, -77, -20, 112, -75, -34, 100, -4, -96, 81, 71, -116, -62, 38, -68, 105, 7, -126, 21, -125, -25, -56, -11, -59, 95, 117, 108, 32, -38, -65, 13, 46, 65, -46, -89, 0, 120, 5, 23, 40, 110, 114, 79, 111, -70, 8, 16, -49, -52, -82, -18, 108, -43, 81, 96, 72, -65, 70, 7, -37, 58, 46, -14},
						{-95, -32, 85, 78, 74, -53, 93, -102, -26, -110, 86, 1, -93, -50, -23, -108, -37, 97, 19, 103, 94, -65, -127, -21, 60, 98, -51, -118, 82, -31, 27, 7, -112, -45, 79, 95, -20, 90, -4, -40, 117, 100, -6, 19, -47, 53, 53, 48, 105, 91, -70, -34, -5, -87, -57, -103, -112, -108, -40, 87, -25, 13, -76, -116},
						{44, -122, -70, 125, -60, -32, 38, 69, -77, -103, 49, -124, -4, 75, -41, -84, 68, 74, 118, 15, -13, 115, 117, -78, 42, 89, 0, -20, -12, -58, -97, 10, -48, 95, 81, 101, 23, -67, -23, 74, -79, 21, 97, 123, 103, 101, -50, -115, 116, 112, 51, 50, -124, 27, 76, 40, 74, 10, 65, -49, 102, 95, 5, 35},
						{-6, 57, 71, 5, -61, -100, -21, -9, 47, -60, 59, 108, -75, 105, 56, 41, -119, 31, 37, 27, -86, 120, -125, -108, 121, 104, -21, -70, -57, -104, 2, 11, 118, 104, 68, 6, 7, -90, -70, -61, -16, 77, -8, 88, 31, -26, 35, -44, 8, 50, 51, -88, -62, -103, 54, -41, -2, 117, 98, -34, 49, -123, 83, -58},
						{54, 21, -36, 126, -50, -72, 82, -5, -122, -116, 72, -19, -18, -68, -71, -27, 97, -22, 53, -94, 47, -6, 15, -92, -55, 127, 13, 13, -69, 81, -82, 8, -50, 10, 84, 110, -87, -44, 61, -78, -65, 84, -32, 48, -8, -105, 35, 116, -68, -116, -6, 75, -77, 120, -95, 74, 73, 105, 39, -87, 98, -53, 47, 10},
						{-113, 116, 37, -1, 95, -89, -93, 113, 36, -70, -57, -99, 94, 52, -81, -118, 98, 58, -36, 73, 82, -67, -80, 46, 83, -127, -8, 73, 66, -27, 43, 7, 108, 32, 73, 1, -56, -108, 41, -98, -15, 49, 1, 107, 65, 44, -68, 126, -28, -53, 120, -114, 126, -79, -14, -105, -33, 53, 5, -119, 67, 52, 35, -29},
						{98, 23, 23, 83, 78, -89, 13, 55, -83, 97, -30, -67, 99, 24, 47, -4, -117, -34, -79, -97, 95, 74, 4, 21, 66, -26, 15, 80, 60, -25, -118, 14, 36, -55, -41, -124, 90, -1, 84, 52, 31, 88, 83, 121, -47, -59, -10, 17, 51, -83, 23, 108, 19, 104, 32, 29, -66, 24, 21, 110, 104, -71, -23, -103},
						{12, -23, 60, 35, 6, -52, -67, 96, 15, -128, -47, -15, 40, 3, 54, -81, 3, 94, 3, -98, -94, -13, -74, -101, 40, -92, 90, -64, -98, 68, -25, 2, -62, -43, 112, 32, -78, -123, 26, -80, 126, 120, -88, -92, 126, -128, 73, -43, 87, -119, 81, 111, 95, -56, -128, -14, 51, -40, -42, 102, 46, 106, 6, 6},
						{-38, -120, -11, -114, -7, -105, -98, 74, 114, 49, 64, -100, 4, 40, 110, -21, 25, 6, -92, -40, -61, 48, 94, -116, -71, -87, 75, -31, 13, -119, 1, 5, 33, -69, -16, -125, -79, -46, -36, 3, 40, 1, -88, -118, -107, 95, -23, -107, -49, 44, -39, 2, 108, -23, 39, 50, -51, -59, -4, -42, -10, 60, 10, -103},
						{67, -53, 55, -32, -117, 3, 94, 52, -115, -127, -109, 116, -121, -27, -115, -23, 98, 90, -2, 48, -54, -76, 108, -56, 99, 30, -35, -18, -59, 25, -122, 3, 43, -13, -109, 34, -10, 123, 117, 113, -112, -85, -119, -62, -78, -114, -96, 101, 72, -98, 28, 89, -98, -121, 20, 115, 89, -20, 94, -55, 124, 27, -76, 94},
						{15, -101, 98, -21, 8, 5, -114, -64, 74, 123, 99, 28, 125, -33, 22, 23, -2, -56, 13, 91, 27, -105, -113, 19, 60, -7, -67, 107, 70, 103, -107, 13, -38, -108, -77, -29, 2, 9, -12, 21, 12, 65, 108, -16, 69, 77, 96, -54, 55, -78, -7, 41, -48, 124, -12, 64, 113, -45, -21, -119, -113, 88, -116, 113},
						{-17, 77, 10, 84, -57, -12, 101, 21, -91, 92, 17, -32, -26, 77, 70, 46, 81, -55, 40, 44, 118, -35, -97, 47, 5, 125, 41, -127, -72, 66, -18, 2, 115, -13, -74, 126, 86, 80, 11, -122, -29, -68, 113, 54, -117, 107, -75, -107, -54, 72, -44, 98, -111, -33, -56, -40, 93, -47, 84, -43, -45, 86, 65, -84},
						{-126, 60, -56, 121, 31, -124, -109, 100, -118, -29, 106, 94, 5, 27, 13, -79, 91, -111, -38, -42, 18, 61, -100, 118, -18, -4, -60, 121, 46, -22, 6, 4, -37, -20, 124, -43, 51, -57, -49, -44, -24, -38, 81, 60, -14, -97, -109, -11, -5, -85, 75, -17, -124, -96, -53, 52, 64, 100, -118, -120, 6, 60, 76, -110},
						{-12, -40, 115, -41, 68, 85, 20, 91, -44, -5, 73, -105, -81, 32, 116, 32, -28, 69, 88, -54, 29, -53, -51, -83, 54, 93, -102, -102, -23, 7, 110, 15, 34, 122, 84, 52, -121, 37, -103, -91, 37, -77, -101, 64, -18, 63, -27, -75, -112, -11, 1, -69, -25, -123, -99, -31, 116, 11, 4, -42, -124, 98, -2, 53},
						{-128, -69, -16, -33, -8, -112, 39, -57, 113, -76, -29, -37, 4, 121, -63, 12, -54, -66, -121, 13, -4, -44, 50, 27, 103, 101, 44, -115, 12, -4, -8, 10, 53, 108, 90, -47, 46, -113, 5, -3, -111, 8, -66, -73, 57, 72, 90, -33, 47, 99, 50, -55, 53, -4, 96, 87, 57, 26, 53, -45, -83, 39, -17, 45},
						{-121, 125, 60, -9, -79, -128, -19, 113, 54, 77, -23, -89, 105, -5, 47, 114, -120, -88, 31, 25, -96, -75, -6, 76, 9, -83, 75, -109, -126, -47, -6, 2, -59, 64, 3, 74, 100, 110, -96, 66, -3, 10, -124, -6, 8, 50, 109, 14, -109, 79, 73, 77, 67, 63, -50, 10, 86, -63, -125, -86, 35, -26, 7, 83},
						{36, 31, -77, 126, 106, 97, 63, 81, -37, -126, 69, -127, -22, -69, 104, -111, 93, -49, 77, -3, -38, -112, 47, -55, -23, -68, -8, 78, -127, -28, -59, 10, 22, -61, -127, -13, -72, -14, -87, 14, 61, 76, -89, 81, -97, -97, -105, 94, -93, -9, -3, -104, -83, 59, 104, 121, -30, 106, -2, 62, -51, -72, -63, 55},
						{81, -88, -8, -96, -31, 118, -23, -38, -94, 80, 35, -20, -93, -102, 124, 93, 0, 15, 36, -127, -41, -19, 6, -124, 94, -49, 44, 26, -69, 43, -58, 9, -18, -3, -2, 60, -122, -30, -47, 124, 71, 47, -74, -68, 4, -101, -16, 77, -120, -15, 45, -12, 68, -77, -74, 63, -113, 44, -71, 56, 122, -59, 53, -44},
						{122, 30, 27, -79, 32, 115, -104, -28, -53, 109, 120, 121, -115, -65, -87, 101, 23, 10, 122, 101, 29, 32, 56, 63, -23, -48, -51, 51, 16, -124, -41, 6, -71, 49, -20, 26, -57, 65, 49, 45, 7, 49, -126, 54, -122, -43, 1, -5, 111, 117, 104, 117, 126, 114, -77, 66, -127, -50, 69, 14, 70, 73, 60, 112},
						{104, -117, 105, -118, 35, 16, -16, 105, 27, -87, -43, -59, -13, -23, 5, 8, -112, -28, 18, -1, 48, 94, -82, 55, 32, 16, 59, -117, 108, -89, 101, 9, -35, 58, 70, 62, 65, 91, 14, -43, -104, 97, 1, -72, 16, -24, 79, 79, -85, -51, -79, -55, -128, 23, 109, -95, 17, 92, -38, 109, 65, -50, 46, -114},
						{44, -3, 102, -60, -85, 66, 121, -119, 9, 82, -47, -117, 67, -28, 108, 57, -47, -52, -24, -82, 65, -13, 85, 107, -21, 16, -24, -85, 102, -92, 73, 5, 7, 21, 41, 47, -118, 72, 43, 51, -5, -64, 100, -34, -25, 53, -45, -115, 30, -72, -114, 126, 66, 60, -24, -67, 44, 48, 22, 117, -10, -33, -89, -108},
						{-7, 71, -93, -66, 3, 30, -124, -116, -48, -76, -7, -62, 125, -122, -60, -104, -30, 71, 36, -110, 34, -126, 31, 10, 108, 102, -53, 56, 104, -56, -48, 12, 25, 21, 19, -90, 45, -122, -73, -112, 97, 96, 115, 71, 127, -7, -46, 84, -24, 102, -104, -96, 28, 8, 37, -84, -13, -65, -6, 61, -85, -117, -30, 70},
						{-112, 39, -39, -24, 127, -115, 68, -1, -111, -43, 101, 20, -12, 39, -70, 67, -50, 68, 105, 69, -91, -106, 91, 4, -52, 75, 64, -121, 46, -117, 31, 10, -125, 77, 51, -3, -93, 58, 79, 121, 126, -29, 56, -101, 1, -28, 49, 16, -80, 92, -62, 83, 33, 17, 106, 89, -9, 60, 79, 38, -74, -48, 119, 24},
						{105, -118, 34, 52, 111, 30, 38, -73, 125, -116, 90, 69, 2, 126, -34, -25, -41, -67, -23, -105, -12, -75, 10, 69, -51, -95, -101, 92, -80, -73, -120, 2, 71, 46, 11, -85, -18, 125, 81, 117, 33, -89, -42, 118, 51, 60, 89, 110, 97, -118, -111, -36, 75, 112, -4, -8, -36, -49, -55, 35, 92, 70, -37, 36},
						{71, 4, -113, 13, -48, 29, -56, 82, 115, -38, -20, -79, -8, 126, -111, 5, -12, -56, -107, 98, 111, 19, 127, -10, -42, 24, -38, -123, 59, 51, -64, 3, 47, -1, -83, -127, -58, 86, 33, -76, 5, 71, -80, -50, -62, 116, 75, 20, -126, 23, -31, -21, 24, -83, -19, 114, -17, 1, 110, -70, -119, 126, 82, -83},
						{-77, -69, -45, -78, -78, 69, 35, 85, 84, 25, -66, -25, 53, -38, -2, 125, -38, 103, 88, 31, -9, -43, 15, -93, 69, -22, -13, -20, 73, 3, -100, 7, 26, -18, 123, -14, -78, 113, 79, -57, -109, -118, 105, -104, 75, -88, -24, -109, 73, -126, 9, 55, 98, -120, 93, 114, 74, 0, -86, -68, 47, 29, 75, 67},
						{-104, 11, -85, 16, -124, -91, 66, -91, 18, -67, -122, -57, -114, 88, 79, 11, -60, -119, 89, 64, 57, 120, -11, 8, 52, -18, -67, -127, 26, -19, -69, 2, -82, -56, 11, -90, -104, 110, -10, -68, 87, 21, 28, 87, -5, -74, -21, -84, 120, 70, -17, 102, 72, -116, -69, 108, -86, -79, -74, 115, -78, -67, 6, 45},
						{-6, -101, -17, 38, -25, -7, -93, 112, 13, -33, 121, 71, -79, -122, -95, 22, 47, -51, 16, 84, 55, -39, -26, 37, -36, -18, 11, 119, 106, -57, 42, 8, -1, 23, 7, -63, -9, -50, 30, 35, -125, 83, 9, -60, -94, -15, -76, 120, 18, -103, -70, 95, 26, 48, -103, -95, 10, 113, 66, 54, -96, -4, 37, 111},
						{-124, -53, 43, -59, -73, 99, 71, -36, -31, 61, -25, -14, -71, 48, 17, 10, -26, -21, -22, 104, 64, -128, 27, -40, 111, -70, -90, 91, -81, -88, -10, 11, -62, 127, -124, -2, -67, -69, 65, 73, 40, 82, 112, -112, 100, -26, 30, 86, 30, 1, -105, 45, 103, -47, -124, 58, 105, 24, 20, 108, -101, 84, -34, 80},
						{28, -1, 84, 111, 43, 109, 57, -23, 52, -95, 110, -50, 77, 15, 80, 85, 125, -117, -10, 8, 59, -58, 18, 97, -58, 45, 92, -3, 56, 24, -117, 9, -73, -9, 48, -99, 50, -24, -3, -41, -43, 48, -77, -8, -89, -42, 126, 73, 28, -65, -108, 54, 6, 34, 32, 2, -73, -123, -106, -52, -73, -106, -112, 109},
						{73, -76, -7, 49, 67, -34, 124, 80, 111, -91, -22, -121, -74, 42, -4, -18, 84, -3, 38, 126, 31, 54, -120, 65, -122, -14, -38, -80, -124, 90, 37, 1, 51, 123, 69, 48, 109, -112, -63, 27, 67, -127, 29, 79, -26, 99, -24, -100, 51, 103, -105, 13, 85, 74, 12, -37, 43, 80, -113, 6, 70, -107, -5, -80},
						{110, -54, 109, 21, -124, 98, 90, -26, 69, -44, 17, 117, 78, -91, -7, -18, -81, -43, 20, 80, 48, -109, 117, 125, -67, 19, -15, 69, -28, 47, 15, 4, 34, -54, 51, -128, 18, 61, -77, -122, 100, -58, -118, -36, 5, 32, 43, 15, 60, -55, 120, 123, -77, -76, -121, 77, 93, 16, -73, 54, 46, -83, -39, 125},
						{115, -15, -42, 111, -124, 52, 29, -124, -10, -23, 41, -128, 65, -60, -121, 6, -42, 14, 98, -80, 80, -46, -38, 64, 16, 84, -50, 47, -97, 11, -88, 12, 68, -127, -92, 87, -22, 54, -49, 33, -4, -68, 21, -7, -45, 84, 107, 57, 8, -106, 0, -87, -104, 93, -43, -98, -92, -72, 110, -14, -66, 119, 14, -68},
						{-19, 7, 7, 66, -94, 18, 36, 8, -58, -31, 21, -113, -124, -5, -12, 105, 40, -62, 57, -56, 25, 117, 49, 17, -33, 49, 105, 113, -26, 78, 97, 2, -22, -84, 49, 67, -6, 33, 89, 28, 30, 12, -3, -23, -45, 7, -4, -39, -20, 25, -91, 55, 53, 21, -94, 17, -54, 109, 125, 124, 122, 117, -125, 60},
						{-28, -104, -46, -22, 71, -79, 100, 48, -90, -57, -30, -23, -24, 1, 2, -31, 85, -6, -113, -116, 105, -31, -109, 106, 1, 78, -3, 103, -6, 100, -44, 15, -100, 97, 59, -42, 22, 83, 113, -118, 112, -57, 80, -45, -86, 72, 77, -26, -106, 50, 28, -24, 41, 22, -73, 108, 18, -93, 30, 8, -11, -16, 124, 106},
						{16, -119, -109, 115, 67, 36, 28, 74, 101, -58, -82, 91, 4, -97, 111, -77, -37, -125, 126, 3, 10, -99, -115, 91, -66, -83, -81, 10, 7, 92, 26, 6, -45, 66, -26, 118, -77, 13, -91, 20, -18, -33, -103, 43, 75, -100, -5, -64, 117, 30, 5, -100, -90, 13, 18, -52, 26, 24, -10, 24, -31, 53, 88, 112},
						{7, -90, 46, 109, -42, 108, -84, 124, -28, -63, 34, -19, -76, 88, -121, 23, 54, -73, -15, -52, 84, -119, 64, 20, 92, -91, -58, -121, -117, -90, -102, 1, 49, 21, 3, -85, -3, 38, 117, 73, -38, -71, -37, 40, -2, -50, -47, -46, 75, -105, 125, 126, -13, 68, 50, -81, -43, -93, 85, -79, 52, 98, 118, 50},
						{-104, 65, -61, 12, 68, 106, 37, -64, 40, -114, 61, 73, 74, 61, -113, -79, 57, 47, -57, -21, -68, -62, 23, -18, 93, -7, -55, -88, -106, 104, -126, 5, 53, 97, 100, -67, -112, -88, 41, 24, 95, 15, 25, -67, 79, -69, 53, 21, -128, -101, 73, 17, 7, -98, 5, -2, 33, -113, 99, -72, 125, 7, 18, -105},
						{-17, 28, 79, 34, 110, 86, 43, 27, -114, -112, -126, -98, -121, 126, -21, 111, 58, -114, -123, 75, 117, -116, 7, 107, 90, 80, -75, -121, 116, -11, -76, 0, -117, -52, 76, -116, 115, -117, 61, -7, 55, -34, 38, 101, 86, -19, -36, -92, -94, 61, 88, -128, -121, -103, 84, 19, -83, -102, 122, -111, 62, 112, 20, 3},
						{-127, -90, 28, -77, -48, -56, -10, 84, -41, 59, -115, 68, -74, -104, -119, -49, -37, -90, -57, 66, 108, 110, -62, -107, 88, 90, 29, -65, 74, -38, 95, 8, 120, 88, 96, -65, -109, 68, -63, -4, -16, 90, 7, 39, -56, -110, -100, 86, -39, -53, -89, -35, 127, -42, -48, -36, 53, -66, 109, -51, 51, -23, -12, 73},
						{-12, 78, 81, 30, 124, 22, 56, -112, 58, -99, 30, -98, 103, 66, 89, 92, -52, -20, 26, 82, -92, -18, 96, 7, 38, 21, -9, -25, -17, 4, 43, 15, 111, 103, -48, -50, -83, 52, 59, 103, 102, 83, -105, 87, 20, -120, 35, -7, -39, -24, 29, -39, -35, -87, 88, 120, 126, 19, 108, 34, -59, -20, 86, 47},
						{19, -70, 36, 55, -42, -49, 33, 100, 105, -5, 89, 43, 3, -85, 60, -96, 43, -46, 86, -33, 120, -123, -99, -100, -34, 48, 82, -37, 34, 78, 127, 12, -39, -76, -26, 117, 74, -60, -68, -2, -37, -56, -6, 94, -27, 81, 32, -96, -19, -32, -77, 22, -56, -49, -38, -60, 45, -69, 40, 106, -106, -34, 101, -75},
						{57, -92, -44, 8, -79, -88, -82, 58, -116, 93, 103, -127, 87, -121, -28, 31, -108, -14, -23, 38, 57, -83, -33, -110, 24, 6, 68, 124, -89, -35, -127, 5, -118, -78, -127, -35, 112, -34, 30, 24, -70, -71, 126, 39, -124, 122, -35, -97, -18, 25, 119, 79, 119, -65, 59, -20, -84, 120, -47, 4, -106, -125, -38, -113},
						{18, -93, 34, -80, -43, 127, 57, -118, 24, -119, 25, 71, 59, -29, -108, -99, -122, 58, 44, 0, 42, -111, 25, 94, -36, 41, -64, -53, -78, -119, 85, 7, -45, -70, 81, -84, 71, -61, -68, -79, 112, 117, 19, 18, 70, 95, 108, -58, 48, 116, -89, 43, 66, 55, 37, -37, -60, 104, 47, -19, -56, 97, 73, 26},
						{78, 4, -111, -36, 120, 111, -64, 46, 99, 125, -5, 97, -126, -21, 60, -78, -33, 89, 25, -60, 0, -49, 59, -118, 18, 3, -60, 30, 105, -92, -101, 15, 63, 50, 25, 2, -116, 78, -5, -25, -59, 74, -116, 64, -55, -121, 1, 69, 51, -119, 43, -6, -81, 14, 5, 84, -67, -73, 67, 24, 82, -37, 109, -93},
						{-44, -30, -64, -68, -21, 74, 124, 122, 114, -89, -91, -51, 89, 32, 96, -1, -101, -112, -94, 98, -24, -31, -50, 100, -72, 56, 24, 30, 105, 115, 15, 3, -67, 107, -18, 111, -38, -93, -11, 24, 36, 73, -23, 108, 14, -41, -65, 32, 51, 22, 95, 41, 85, -121, -35, -107, 0, 105, -112, 59, 48, -22, -84, 46},
						{4, 38, 54, -84, -78, 24, -48, 8, -117, 78, -95, 24, 25, -32, -61, 26, -97, -74, 46, -120, -125, 27, 73, 107, -17, -21, -6, -52, 47, -68, 66, 5, -62, -12, -102, -127, 48, -69, -91, -81, -33, -13, -9, -12, -44, -73, 40, -58, 120, -120, 108, 101, 18, -14, -17, -93, 113, 49, 76, -4, -113, -91, -93, -52},
						{28, -48, 70, -35, 123, -31, 16, -52, 72, 84, -51, 78, 104, 59, -102, -112, 29, 28, 25, 66, 12, 75, 26, -85, 56, -12, -4, -92, 49, 86, -27, 12, 44, -63, 108, 82, -76, -97, -41, 95, -48, -95, -115, 1, 64, -49, -97, 90, 65, 46, -114, -127, -92, 79, 100, 49, 116, -58, -106, 9, 117, -7, -91, 96},
						{58, 26, 18, 76, 127, -77, -58, -87, -116, -44, 60, -32, -4, -76, -124, 4, -60, 82, -5, -100, -95, 18, 2, -53, -50, -96, -126, 105, 93, 19, 74, 13, 87, 125, -72, -10, 42, 14, 91, 44, 78, 52, 60, -59, -27, -37, -57, 17, -85, 31, -46, 113, 100, -117, 15, 108, -42, 12, 47, 63, 1, 11, -122, -3}};
				
				for (int i = 0; i < recipients.length; i++) {
					
					Transaction transaction = new Transaction(Transaction.TYPE_PAYMENT, Transaction.SUBTYPE_PAYMENT_ORDINARY_PAYMENT, 0, (short)0, new byte[]{18, 89, -20, 33, -45, 26, 48, -119, -115, 124, -47, 96, -97, -128, -39, 102, -117, 71, 120, -29, -39, 126, -108, 16, 68, -77, -97, 12, 68, -46, -27, 27}, recipients[i], amounts[i], 0, 0, signatures[i]);
					
					transactions.put(transaction.getId(), transaction);
					
				}
				
				for (Transaction transaction : transactions.values()) {
					transaction.index = transactionCounter.incrementAndGet();
					transaction.block = GENESIS_BLOCK_ID;
					
				}
				
				Transaction.saveTransactions("transactions.nxt");
				
			}
			
			try {
				
				logMessage("Loading blocks...");
				Block.loadBlocks("blocks.nxt");
				logMessage("...Done");
				
			} catch (FileNotFoundException e) {
				
				blocks.clear();
				
				Block block = new Block(-1, 0, 0, transactions.size(), 1000000000, 0, transactions.size() * 128, null, new byte[]{18, 89, -20, 33, -45, 26, 48, -119, -115, 124, -47, 96, -97, -128, -39, 102, -117, 71, 120, -29, -39, 126, -108, 16, 68, -77, -97, 12, 68, -46, -27, 27}, new byte[64], new byte[]{105, -44, 38, -60, -104, -73, 10, -58, -47, 103, -127, -128, 53, 101, 39, -63, -2, -32, 48, -83, 115, 47, -65, 118, 114, -62, 38, 109, 22, 106, 76, 8, -49, -113, -34, -76, 82, 79, -47, -76, -106, -69, -54, -85, 3, -6, 110, 103, 118, 15, 109, -92, 82, 37, 20, 2, 36, -112, 21, 72, 108, 72, 114, 17});
				block.index = blockCounter.incrementAndGet();
				blocks.put(GENESIS_BLOCK_ID, block);
				
				block.transactions = new long[block.numberOfTransactions];
				int i = 0;
				for (long transaction : transactions.keySet()) {
					
					block.transactions[i++] = transaction;
					
				}
				Arrays.sort(block.transactions);
				MessageDigest digest = MessageDigest.getInstance("SHA-256");
				for (i = 0; i < block.numberOfTransactions; i++) {
					
					digest.update(transactions.get(block.transactions[i]).getBytes());
					
				}
				block.payloadHash = digest.digest();
				
				block.baseTarget = initialBaseTarget;
				lastBlock = GENESIS_BLOCK_ID;
				block.cumulativeDifficulty = BigInteger.ZERO;
				
				Block.saveBlocks("blocks.nxt", false);
				
			}
			
			logMessage("Scanning blockchain...");
            Map<Long,Block> loadedBlocks = new HashMap<>(blocks);
            blocks.clear();
			lastBlock = GENESIS_BLOCK_ID;
			long curBlockId = GENESIS_BLOCK_ID;
			do {
				
				Block curBlock = loadedBlocks.get(curBlockId);
				long nextBlockId = curBlock.nextBlock;
				curBlock.analyze();
				curBlockId = nextBlockId;
				
			} while (curBlockId != 0);
			logMessage("...Done");
			
			scheduledThreadPool.scheduleWithFixedDelay(new Runnable() {
				
				@Override
				public void run() {
					
					try {
						
						if (Peer.getNumberOfConnectedPublicPeers() < Nxt.maxNumberOfConnectedPublicPeers) {
							
							Peer peer = Peer.getAnyPeer(ThreadLocalRandom.current().nextInt(2) == 0 ? Peer.STATE_NONCONNECTED : Peer.STATE_DISCONNECTED, false);
							if (peer != null) {
								
								peer.connect();
								
							}
							
						}
						
					} catch (Exception e) { }
					
				}
				
			}, 0, 5, TimeUnit.SECONDS);
			
			scheduledThreadPool.scheduleWithFixedDelay(new Runnable() {
				
				@Override
				public void run() {
					
					try {
						
						long curTime = System.currentTimeMillis();
						
						for (Peer peer : Nxt.peers.values()) {
							
							if (peer.blacklistingTime > 0 && peer.blacklistingTime + Nxt.blacklistingPeriod <= curTime ) {
								
								peer.removeBlacklistedStatus();
								
							}
							
						}
						
					} catch (Exception e) { }
					
				}
				
			}, 0, 1, TimeUnit.SECONDS);
			
			scheduledThreadPool.scheduleWithFixedDelay(new Runnable() {

                private final JSONObject getPeersRequest = new JSONObject();
                {
                    getPeersRequest.put("requestType", "getPeers");
                }

				@Override
				public void run() {
					
					try {
						
						Peer peer = Peer.getAnyPeer(Peer.STATE_CONNECTED, true);
						if (peer != null) {
							JSONObject response = peer.send(getPeersRequest);
							if (response != null) {
								
								JSONArray peers = (JSONArray)response.get("peers");
								for (int i = 0; i < peers.size(); i++) {
									
									String address = ((String)peers.get(i)).trim(); 
									if (address.length() > 0) {
										//TODO: can a rogue peer fill the peer pool with zombie addresses? consider an option to trust only highly-hallmarked peers
										Peer.addPeer(address, address);
										
									}
									
								}
								
							}
							
						}
						
					} catch (Exception e) { }
					
				}
				
			}, 0, 5, TimeUnit.SECONDS);
			
			scheduledThreadPool.scheduleWithFixedDelay(new Runnable() {

                private final JSONObject getUnconfirmedTransactionsRequest = new JSONObject();
                {
                    getUnconfirmedTransactionsRequest.put("requestType", "getUnconfirmedTransactions");
                }

				@Override
				public void run() {
					
					try {
						
						Peer peer = Peer.getAnyPeer(Peer.STATE_CONNECTED, true);
						if (peer != null) {
							
							JSONObject response = peer.send(getUnconfirmedTransactionsRequest);
							if (response != null) {
								
								Transaction.processTransactions(response, "unconfirmedTransactions");
								
							}
							
						}
						
					} catch (Exception e) { }
					
				}
				
			}, 0, 5, TimeUnit.SECONDS);
			
			scheduledThreadPool.scheduleWithFixedDelay(new Runnable() {
				
				@Override
				public void run() {
					
					try {
						
						int curTime = getEpochTime(System.currentTimeMillis());
                        JSONArray removedUnconfirmedTransactions = new JSONArray();

                        Iterator<Transaction> iterator = unconfirmedTransactions.values().iterator();
                        while (iterator.hasNext()) {

                            Transaction transaction = iterator.next();
                            if (transaction.timestamp + transaction.deadline * 60 < curTime || !transaction.validateAttachment()) {

                                iterator.remove();

                                Account account = accounts.get(Account.getId(transaction.senderPublicKey));
                                synchronized (account) {

                                    account.setUnconfirmedBalance(account.unconfirmedBalance + (transaction.amount + transaction.fee) * 100L);

                                }

                                JSONObject removedUnconfirmedTransaction = new JSONObject();
                                removedUnconfirmedTransaction.put("index", transaction.index);
                                removedUnconfirmedTransactions.add(removedUnconfirmedTransaction);

                            }

                        }

                        if (removedUnconfirmedTransactions.size() > 0) {

                            JSONObject response = new JSONObject();
                            response.put("response", "processNewData");

                            response.put("removedUnconfirmedTransactions", removedUnconfirmedTransactions);

                            for (User user : users.values()) {

                                user.send(response);

                            }

                        }

					} catch (Exception e) { }
					
				}
				
			}, 0, 1, TimeUnit.SECONDS);
			
			scheduledThreadPool.scheduleWithFixedDelay(new Runnable() {

                private final JSONObject getCumulativeDifficultyRequest = new JSONObject();
                private final JSONObject getMilestoneBlockIdsRequest = new JSONObject();
                {
                    getCumulativeDifficultyRequest.put("requestType", "getCumulativeDifficulty");
                    getMilestoneBlockIdsRequest.put("requestType", "getMilestoneBlockIds");
                }
				
				@Override
				public void run() {
					
					try {
						
						Peer peer = Peer.getAnyPeer(Peer.STATE_CONNECTED, true);
						if (peer != null) {

							lastBlockchainFeeder = peer;
							
							JSONObject response = peer.send(getCumulativeDifficultyRequest);
							if (response != null) {
								
								BigInteger curCumulativeDifficulty = Block.getLastBlock().cumulativeDifficulty, betterCumulativeDifficulty = new BigInteger((String)response.get("cumulativeDifficulty"));
								if (betterCumulativeDifficulty.compareTo(curCumulativeDifficulty) > 0) {
									
									response = peer.send(getMilestoneBlockIdsRequest);
									if (response != null) {
										
										long commonBlockId = GENESIS_BLOCK_ID;
										
										JSONArray milestoneBlockIds = (JSONArray)response.get("milestoneBlockIds");
										for (int i = 0; i < milestoneBlockIds.size(); i++) {
											
											long blockId = (new BigInteger((String)milestoneBlockIds.get(i))).longValue();
											Block block = blocks.get(blockId);
											if (block != null) {
												
												commonBlockId = blockId;
												
												break;
												
											}
											
										}
										
										int i, numberOfBlocks;
										do {
											
											JSONObject request = new JSONObject();
											request.put("requestType", "getNextBlockIds");
											request.put("blockId", convert(commonBlockId));
											response = peer.send(request);
											if (response == null) {
												
												return;
												
											} else {
												
												JSONArray nextBlockIds = (JSONArray)response.get("nextBlockIds");
												numberOfBlocks = nextBlockIds.size();
												if (numberOfBlocks == 0) {
													
													return;
													
												} else {
													
													long blockId;
													for (i = 0; i < numberOfBlocks; i++) {
														
														blockId = (new BigInteger((String)nextBlockIds.get(i))).longValue();
														if (blocks.get(blockId) == null) {
															
															break;
															
														}
														
														commonBlockId = blockId;
														
													}
													
												}
												
											}
											
										} while (i == numberOfBlocks);
										
										if (Block.getLastBlock().height - blocks.get(commonBlockId).height < 720) {
											
											long curBlockId = commonBlockId;
											LinkedList<Block> futureBlocks = new LinkedList<>();
											HashMap<Long, Transaction> futureTransactions = new HashMap<>();
											
											do {
												
												JSONObject request = new JSONObject();
												request.put("requestType", "getNextBlocks");
												request.put("blockId", convert(curBlockId));
												response = peer.send(request);
												if (response == null) {
													
													break;
													
												} else {
													
													JSONArray nextBlocks = (JSONArray)response.get("nextBlocks");
													numberOfBlocks = nextBlocks.size();
													if (numberOfBlocks == 0) {
														
														break;
														
													} else {
														
														for (i = 0; i < numberOfBlocks; i++) {
															
															JSONObject blockData = (JSONObject)nextBlocks.get(i);
															Block block = Block.getBlock(blockData);
															curBlockId = block.getId();

															synchronized (blocksAndTransactionsLock) {
																
																boolean alreadyPushed = false;
																if (block.previousBlock == lastBlock) {
																	
																	ByteBuffer buffer = ByteBuffer.allocate(BLOCK_HEADER_LENGTH + block.payloadLength);
																	buffer.order(ByteOrder.LITTLE_ENDIAN);
																	buffer.put(block.getBytes());
																	
																	JSONArray transactionsData = (JSONArray)blockData.get("transactions");
																	for (Object transaction : transactionsData) {
																		
																		buffer.put(Transaction.getTransaction((JSONObject)transaction).getBytes());
																		
																	}
																	
																	if (Block.pushBlock(buffer, false)) {
																		
																		alreadyPushed = true;
																		
																	} else {
																		
																		peer.blacklist();
																		
																		return;
																		
																	}
																	
																}
																if (!alreadyPushed && blocks.get(block.getId()) == null) {
																	
																	futureBlocks.add(block);
																	
																	block.transactions = new long[block.numberOfTransactions];
																	JSONArray transactionsData = (JSONArray)blockData.get("transactions");
																	for (int j = 0; j < block.numberOfTransactions; j++) {
																		
																		Transaction transaction = Transaction.getTransaction((JSONObject)transactionsData.get(j));
																		block.transactions[j] = transaction.getId();
																		futureTransactions.put(block.transactions[j], transaction);
																		
																	}
																	
																}
																
															}
															
														}
														
													}
													
												}
												
											} while (true);
											
											if (!futureBlocks.isEmpty() && Block.getLastBlock().height - blocks.get(commonBlockId).height < 720) {
												
												synchronized (blocksAndTransactionsLock) {
													
													Block.saveBlocks("blocks.nxt.bak", true);
													Transaction.saveTransactions("transactions.nxt.bak");
													
													curCumulativeDifficulty = Block.getLastBlock().cumulativeDifficulty;
													
													while (lastBlock != commonBlockId && Block.popLastBlock()) { }
													
													if (lastBlock == commonBlockId) {
														
														for (Block block : futureBlocks) {
															
															if (block.previousBlock == lastBlock) {
																
																ByteBuffer buffer = ByteBuffer.allocate(BLOCK_HEADER_LENGTH + block.payloadLength);
																buffer.order(ByteOrder.LITTLE_ENDIAN);
																buffer.put(block.getBytes());
																
																for (int j = 0; j < block.transactions.length; j++) {
																	
																	buffer.put(futureTransactions.get(block.transactions[j]).getBytes());
																	
																}
																
																if (!Block.pushBlock(buffer, false)) {
																	
																	break;
																	
																}
																
															}
															
														}
														
													}
													
													if (Block.getLastBlock().cumulativeDifficulty.compareTo(curCumulativeDifficulty) < 0) {
														
														Block.loadBlocks("blocks.nxt.bak");
														Transaction.loadTransactions("transactions.nxt.bak");
														
														peer.blacklist();
														
													}
													
												}
												
											}

                                            synchronized (blocksAndTransactionsLock) {
    											Block.saveBlocks("blocks.nxt", false);
	    										Transaction.saveTransactions("transactions.nxt");
                                            }

										}
										
									}
									
								}
								
							}
							
						}
						
					} catch (Exception e) { }
					
				}
				
			}, 0, 1, TimeUnit.SECONDS);
			
			scheduledThreadPool.scheduleWithFixedDelay(new Runnable() {
				
				@Override
				public void run() {
					
					try {
						
						HashMap<Account, User> unlockedAccounts = new HashMap<>();
						for (User user : users.values()) {
							
							if (user.secretPhrase != null) {
								
								Account account = accounts.get(Account.getId(Crypto.getPublicKey(user.secretPhrase)));
								if (account != null && account.getEffectiveBalance() > 0) {
									
									unlockedAccounts.put(account, user);
									
								}
								
							}
							
						}
						
						for (Map.Entry<Account, User> unlockedAccountEntry : unlockedAccounts.entrySet()) {
							
							Account account = unlockedAccountEntry.getKey();
							User user = unlockedAccountEntry.getValue();
							Block lastBlock = Block.getLastBlock();
							if (lastBlocks.get(account) != lastBlock) {
								
								MessageDigest digest = MessageDigest.getInstance("SHA-256");
								byte[] generationSignatureHash;
								if (lastBlock.height < TRANSPARENT_FORGING_BLOCK) {
									
									byte[] generationSignature = Crypto.sign(lastBlock.generationSignature, user.secretPhrase);
									generationSignatureHash = digest.digest(generationSignature);
									
								} else {
									
									digest.update(lastBlock.generationSignature);
									generationSignatureHash = digest.digest(Crypto.getPublicKey(user.secretPhrase));
									
								}
								BigInteger hit = new BigInteger(1, new byte[] {generationSignatureHash[7], generationSignatureHash[6], generationSignatureHash[5], generationSignatureHash[4], generationSignatureHash[3], generationSignatureHash[2], generationSignatureHash[1], generationSignatureHash[0]});
								
								lastBlocks.put(account, lastBlock);
								hits.put(account, hit);
								
								JSONObject response = new JSONObject();
								response.put("response", "setBlockGenerationDeadline");
								response.put("deadline", hit.divide(BigInteger.valueOf(Block.getBaseTarget()).multiply(BigInteger.valueOf(account.getEffectiveBalance()))).longValue() - (getEpochTime(System.currentTimeMillis()) - lastBlock.timestamp));
								
                                // there may be multiple User entries for the same secretPhrase/account in the users map
                                // because of multiple logins, ideally the duplicates should be removed on account unlock
                                // so this is a temporary fix (or better, keep a separate miningUsers map with no duplicates)
                                for (User u : users.values()) {
                                    if (user.secretPhrase.equals(u.secretPhrase)) {
                                        u.send(response);
                                    }
                                }

							}
							
							int elapsedTime = getEpochTime(System.currentTimeMillis()) - lastBlock.timestamp;
							if (elapsedTime > 0) {
								
								BigInteger target = BigInteger.valueOf(Block.getBaseTarget()).multiply(BigInteger.valueOf(account.getEffectiveBalance())).multiply(BigInteger.valueOf(elapsedTime));
								if (hits.get(account).compareTo(target) < 0) {
									
									account.generateBlock(user.secretPhrase);
									
								}
								
							}
							
						}
						
					} catch (Exception e) { }
					
				}
				
			}, 0, 1, TimeUnit.SECONDS);
			
		} catch (Exception e) {
			
			logMessage("10: " + e.toString());

		}
		
	}
	
    //TODO: the huge switch statement should be refactored completely, a separate class should handle each case
    // This is required in order to be able to factor out closed-source code into separate class files
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		User user = null;
		
		try {
			
			String userPasscode = req.getParameter("user");
			if (userPasscode == null) {
				
				JSONObject response = new JSONObject();
				
				if (allowedBotHosts != null && !allowedBotHosts.contains(req.getRemoteHost())) {
					
					response.put("errorCode", 7);
					response.put("errorDescription", "Not allowed");
					
				} else {
					
					String requestType = req.getParameter("requestType");
					if (requestType == null) {
						
						response.put("errorCode", 1);
						response.put("errorDescription", "Incorrect request");
						
					} else {
						
						switch (requestType) {
						
						case "assignAlias":
							{
								
								String secretPhrase = req.getParameter("secretPhrase");
								String alias = req.getParameter("alias");
								String uri = req.getParameter("uri");
								String feeValue = req.getParameter("fee");
								String deadlineValue = req.getParameter("deadline");
								String referencedTransactionValue = req.getParameter("referencedTransaction");
								if (secretPhrase == null) {
									
									response.put("errorCode", 3);
									response.put("errorDescription", "\"secretPhrase\" not specified");
									
								} else if (alias == null) {
									
									response.put("errorCode", 3);
									response.put("errorDescription", "\"alias\" not specified");
									
								} else if (uri == null) {
									
									response.put("errorCode", 3);
									response.put("errorDescription", "\"uri\" not specified");
									
								} else if (feeValue == null) {
									
									response.put("errorCode", 3);
									response.put("errorDescription", "\"fee\" not specified");
									
								} else if (deadlineValue == null) {
									
									response.put("errorCode", 3);
									response.put("errorDescription", "\"deadline\" not specified");
									
								} else {
									
									alias = alias.trim();
									if (alias.length() == 0 || alias.length() > 100) {
										
										response.put("errorCode", 4);
										response.put("errorDescription", "Incorrect \"alias\" (length must be in [1..100] range)");
										
									} else {
										
										String normalizedAlias = alias.toLowerCase();
										int i;
										for (i = 0; i < normalizedAlias.length(); i++) {
											
											if (alphabet.indexOf(normalizedAlias.charAt(i)) < 0) {
												
												break;
												
											}
											
										}
										if (i != normalizedAlias.length()) {
											
											response.put("errorCode", 4);
											response.put("errorDescription", "Incorrect \"alias\" (must contain only digits and latin letters)");
											
										} else {
											
											uri = uri.trim();
											if (uri.length() > 1000) {
												
												response.put("errorCode", 4);
												response.put("errorDescription", "Incorrect \"uri\" (length must be not longer than 1000 characters)");
												
											} else {
												
												try {
													
													int fee = Integer.parseInt(feeValue);
													if (fee <= 0 || fee >= MAX_BALANCE) {
														
														throw new Exception();
														
													}
													
													try {
														
														short deadline = Short.parseShort(deadlineValue);
														if (deadline < 1) {
															
															throw new Exception();
                                                            //TODO: better error checking
															
														}
														
														long referencedTransaction = referencedTransactionValue == null ? 0 : (new BigInteger(referencedTransactionValue)).longValue();
														
														byte[] publicKey = Crypto.getPublicKey(secretPhrase);
														long accountId = Account.getId(publicKey);
														Account account = accounts.get(accountId);
														if (account == null) {
															
															response.put("errorCode", 6);
															response.put("errorDescription", "Not enough funds");
															
														} else {
															
															if (fee * 100L > account.unconfirmedBalance) {
																
																response.put("errorCode", 6);
																response.put("errorDescription", "Not enough funds");
																
															} else {
																
																Alias aliasData = aliases.get(normalizedAlias);
																if (aliasData != null && aliasData.account != account) {
																	
																	response.put("errorCode", 8);
																	response.put("errorDescription", "\"" + alias + "\" is already used");
																	
																} else {
																	
																	int timestamp = getEpochTime(System.currentTimeMillis());
																	
																	Transaction transaction = new Transaction(Transaction.TYPE_MESSAGING, Transaction.SUBTYPE_MESSAGING_ALIAS_ASSIGNMENT, timestamp, deadline, publicKey, CREATOR_ID, 0, fee, referencedTransaction, new byte[64]);
																	transaction.attachment = new Transaction.MessagingAliasAssignmentAttachment(alias, uri);
																	transaction.sign(secretPhrase);
																	
																	JSONObject peerRequest = new JSONObject();
																	peerRequest.put("requestType", "processTransactions");
																	JSONArray transactionsData = new JSONArray();
																	transactionsData.add(transaction.getJSONObject());
																	peerRequest.put("transactions", transactionsData);
																	
																	Peer.sendToAllPeers(peerRequest);
																	
																	response.put("transaction", convert(transaction.getId()));
																	
																}
																
															}
															
														}
														
													} catch (Exception e) {
														
														response.put("errorCode", 4);
														response.put("errorDescription", "Incorrect \"deadline\"");
														
													}
													
												} catch (Exception e) {
													
													response.put("errorCode", 4);
													response.put("errorDescription", "Incorrect \"fee\"");
													
												}
												
											}
											
										}
										
									}
									
								}
								
							}
							break;
							
						case "broadcastTransaction":
							{
								
								String transactionBytes = req.getParameter("transactionBytes");
								if (transactionBytes == null) {
									
									response.put("errorCode", 3);
									response.put("errorDescription", "\"transactionBytes\" not specified");
									
								} else {
									
									try {
										
										ByteBuffer buffer = ByteBuffer.wrap(convert(transactionBytes));
										buffer.order(ByteOrder.LITTLE_ENDIAN);
										Transaction transaction = Transaction.getTransaction(buffer);
										
										JSONObject peerRequest = new JSONObject();
										peerRequest.put("requestType", "processTransactions");
										JSONArray transactionsData = new JSONArray();
										transactionsData.add(transaction.getJSONObject());
										peerRequest.put("transactions", transactionsData);
										
										Peer.sendToAllPeers(peerRequest);
										
										response.put("transaction", convert(transaction.getId()));
										
									} catch (Exception e) {
										
										response.put("errorCode", 4);
										response.put("errorDescription", "Incorrect \"transactionBytes\"");
										
									}
									
								}
								
							}
							break;
							
						case "decodeHallmark":
							{
								
								String hallmarkValue = req.getParameter("hallmark");
								if (hallmarkValue == null) {
									
									response.put("errorCode", 3);
									response.put("errorDescription", "\"hallmark\" not specified");
									
								} else {
									
									try {
										
										byte[] hallmark = convert(hallmarkValue);
										
										ByteBuffer buffer = ByteBuffer.wrap(hallmark);
										buffer.order(ByteOrder.LITTLE_ENDIAN);
										
										byte[] publicKey = new byte[32];
										buffer.get(publicKey);
										int hostLength = buffer.getShort();
										byte[] hostBytes = new byte[hostLength];
										buffer.get(hostBytes);
										String host = new String(hostBytes, "UTF-8");
										int weight = buffer.getInt();
										int date = buffer.getInt();
										buffer.get();
										byte[] signature = new byte[64];
										buffer.get(signature);
										
										response.put("account", convert(Account.getId(publicKey)));
										response.put("host", host);
										response.put("weight", weight);
										int year = date / 10000;
										int month = (date % 10000) / 100;
										int day = date % 100;
										response.put("date", (year < 10 ? "000" : (year < 100 ? "00" : (year < 1000 ? "0" : ""))) + year + "-" + (month < 10 ? "0" : "") + month + "-" + (day < 10 ? "0" : "") + day);
										byte[] data = new byte[hallmark.length - 64];
										System.arraycopy(hallmark, 0, data, 0, data.length);
										response.put("valid", host.length() > 100 || weight <= 0 || weight > MAX_BALANCE ? false : Crypto.verify(signature, data, publicKey));
										
									} catch (Exception e) {
										
										response.put("errorCode", 4);
										response.put("errorDescription", "Incorrect \"hallmark\"");
										
									}
									
								}
								
							}
							break;
							
						case "decodeToken":
							{
								
								String website = req.getParameter("website");
								String token = req.getParameter("token");
								if (website == null) {
									
									response.put("errorCode", 3);
									response.put("errorDescription", "\"website\" not specified");
									
								} else if (token == null) {
									
									response.put("errorCode", 3);
									response.put("errorDescription", "\"token\" not specified");
									
								} else {
									
									byte[] websiteBytes = website.trim().getBytes("UTF-8");
									byte[] tokenBytes = new byte[100];
									int i = 0, j = 0;
									try {
										
										for (; i < token.length(); i += 8, j += 5) {
											
											long number = Long.parseLong(token.substring(i, i + 8), 32);
											tokenBytes[j] = (byte)number;
											tokenBytes[j + 1] = (byte)(number >> 8);
											tokenBytes[j + 2] = (byte)(number >> 16);
											tokenBytes[j + 3] = (byte)(number >> 24);
											tokenBytes[j + 4] = (byte)(number >> 32);
											
										}
										
									} catch (Exception e) { }
									
									if (i != 160) {
										
										response.put("errorCode", 4);
										response.put("errorDescription", "Incorrect \"token\"");
										
									} else {
										
										byte[] publicKey = new byte[32];
										System.arraycopy(tokenBytes, 0, publicKey, 0, 32);
										int timestamp = (tokenBytes[32] & 0xFF) | ((tokenBytes[33] & 0xFF) << 8) | ((tokenBytes[34] & 0xFF) << 16) | ((tokenBytes[35] & 0xFF) << 24);
										byte[] signature = new byte[64];
										System.arraycopy(tokenBytes, 36, signature, 0, 64);
										
										byte[] data = new byte[websiteBytes.length + 36];
										System.arraycopy(websiteBytes, 0, data, 0, websiteBytes.length);
										System.arraycopy(tokenBytes, 0, data, websiteBytes.length, 36);
										boolean valid = Crypto.verify(signature, data, publicKey);
										
										response.put("account", convert(Account.getId(publicKey)));
										response.put("timestamp", timestamp);
										response.put("valid", valid);
										
									}
									
								}
								
							}
							break;
							
						case "getAccountId":
							{
								
								String secretPhrase = req.getParameter("secretPhrase");
								if (secretPhrase == null) {
									
									response.put("errorCode", 3);
									response.put("errorDescription", "\"secretPhrase\" not specified");
									
								} else {
									
									byte[] publicKeyHash = MessageDigest.getInstance("SHA-256").digest(Crypto.getPublicKey(secretPhrase));
									BigInteger bigInteger = new BigInteger(1, new byte[] {publicKeyHash[7], publicKeyHash[6], publicKeyHash[5], publicKeyHash[4], publicKeyHash[3], publicKeyHash[2], publicKeyHash[1], publicKeyHash[0]});
									response.put("accountId", bigInteger.toString());
									
								}
								
							}
							break;
							
						case "getAccountPublicKey":
							{
								
								String account = req.getParameter("account");
								if (account == null) {
									
									response.put("errorCode", 3);
									response.put("errorDescription", "\"account\" not specified");
									
								} else {
									
									try {
										
										long accountId = (new BigInteger(account)).longValue();
										Account accountData = accounts.get(accountId);
										if (accountData == null) {
											
											response.put("errorCode", 5);
											response.put("errorDescription", "Unknown account");
											
										} else {
											
											if (accountData.publicKey != null) {
												
												response.put("publicKey", convert(accountData.publicKey));
												
											}
											
										}
										
									} catch (Exception e) {
										
										response.put("errorCode", 4);
										response.put("errorDescription", "Incorrect \"account\"");
										
									}
									
								}
								
							}
							break;
							
						case "getAccountTransactionIds":
							{
								
								String account = req.getParameter("account");
								String timestampValue = req.getParameter("timestamp");
								if (account == null) {
									
									response.put("errorCode", 3);
									response.put("errorDescription", "\"account\" not specified");
									
								} else if (timestampValue == null) {
									
									response.put("errorCode", 3);
									response.put("errorDescription", "\"timestamp\" not specified");
									
								} else {
									
									try {
										
										Account accountData = accounts.get((new BigInteger(account)).longValue());
										if (accountData == null) {
											
											response.put("errorCode", 5);
											response.put("errorDescription", "Unknown account");
											
										} else {
											
											try {
												
												int timestamp = Integer.parseInt(timestampValue);
												if (timestamp < 0) {
													
													throw new Exception();
													
												}
												
												JSONArray transactionIds = new JSONArray();
												for (Map.Entry<Long, Transaction> transactionEntry : transactions.entrySet()) {
													
													Transaction transaction = transactionEntry.getValue();
													if (blocks.get(transaction.block).timestamp >= timestamp && (Account.getId(transaction.senderPublicKey) == accountData.id || transaction.recipient == accountData.id)) {
														
														transactionIds.add(convert(transactionEntry.getKey()));
														
													}
													
												}
												response.put("transactionIds", transactionIds);
												
											} catch (Exception e) {
												
												response.put("errorCode", 4);
												response.put("errorDescription", "Incorrect \"timestamp\"");
												
											}
											
										}
										
									} catch (Exception e) {
										
										response.put("errorCode", 4);
										response.put("errorDescription", "Incorrect \"account\"");
										
									}
									
								}
								
							}
							break;
							
						case "getAlias":
							{
								
								String alias = req.getParameter("alias");
								if (alias == null) {
									
									response.put("errorCode", 3);
									response.put("errorDescription", "\"alias\" not specified");
									
								} else {
									
									try {
										
										long aliasId = (new BigInteger(alias)).longValue();
										Alias aliasData = aliasIdToAliasMappings.get(aliasId);
										if (aliasData == null) {
											
											response.put("errorCode", 5);
											response.put("errorDescription", "Unknown alias");
											
										} else {
											
											response.put("account", convert(aliasData.account.id));
											response.put("alias", aliasData.alias);
											if (aliasData.uri.length() > 0) {
												
												response.put("uri", aliasData.uri);
												
											}
											response.put("timestamp", aliasData.timestamp);
											
										}
										
									} catch (Exception e) {
										
										response.put("errorCode", 4);
										response.put("errorDescription", "Incorrect \"alias\"");
										
									}
									
								}
								
							}
							break;
							
						case "getAliasIds":
							{
								
								String timestampValue = req.getParameter("timestamp");
								if (timestampValue == null) {
									
									response.put("errorCode", 3);
									response.put("errorDescription", "\"timestamp\" not specified");
									
								} else {
									
									try {
										
										int timestamp = Integer.parseInt(timestampValue);
										if (timestamp < 0) {
											
											throw new Exception();
											
										}
										
										JSONArray aliasIds = new JSONArray();
										for (Map.Entry<Long, Alias> aliasEntry : aliasIdToAliasMappings.entrySet()) {
											
											if (aliasEntry.getValue().timestamp >= timestamp) {
												
												aliasIds.add(convert(aliasEntry.getKey()));
												
											}
											
										}
										response.put("aliasIds", aliasIds);
										
									} catch (Exception e) {
										
										response.put("errorCode", 4);
										response.put("errorDescription", "Incorrect \"timestamp\"");
										
									}
									
								}
								
							}
							break;
							
						case "getAliasURI":
							{
								
								String alias = req.getParameter("alias");
								if (alias == null) {
									
									response.put("errorCode", 3);
									response.put("errorDescription", "\"alias\" not specified");
									
								} else {
									
									Alias aliasData = aliases.get(alias.toLowerCase());
									if (aliasData == null) {
										
										response.put("errorCode", 5);
										response.put("errorDescription", "Unknown alias");
										
									} else {
										
										if (aliasData.uri.length() > 0) {
											
											response.put("uri", aliasData.uri);
											
										}
										
									}
									
								}
								
								/*String origin = req.getHeader("Origin");
								if (origin != null) {
									
									resp.setHeader("Access-Control-Allow-Origin", origin);
									
								}*/
								
							}
							break;
							
						case "getBalance":
							{
								
								String account = req.getParameter("account");
								if (account == null) {
									
									response.put("errorCode", 3);
									response.put("errorDescription", "\"account\" not specified");
									
								} else {
									
									try {
										
										Account accountData = accounts.get((new BigInteger(account)).longValue());
										if (accountData == null) {
											
											response.put("balance", 0);
											response.put("unconfirmedBalance", 0);
											response.put("effectiveBalance", 0);
											
										} else {
											
											synchronized (accountData) {
												
												response.put("balance", accountData.balance);
												response.put("unconfirmedBalance", accountData.unconfirmedBalance);
												response.put("effectiveBalance", accountData.getEffectiveBalance() * 100L);
												
											}
											
										}
										
									} catch (Exception e) {
										
										response.put("errorCode", 4);
										response.put("errorDescription", "Incorrect \"account\"");
										
									}
									
								}
								
							}
							break;
							
						case "getBlock":
							{
								
								String block = req.getParameter("block");
								if (block == null) {
									
									response.put("errorCode", 3);
									response.put("errorDescription", "\"block\" not specified");
									
								} else {
									
									try {
										
										Block blockData = blocks.get((new BigInteger(block)).longValue());
										if (blockData == null) {
											
											response.put("errorCode", 5);
											response.put("errorDescription", "Unknown block");
											
										} else {
											
											response.put("height", blockData.height);
											response.put("generator", convert(Account.getId(blockData.generatorPublicKey)));
											response.put("timestamp", blockData.timestamp);
											response.put("numberOfTransactions", blockData.numberOfTransactions);
											response.put("totalAmount", blockData.totalAmount);
											response.put("totalFee", blockData.totalFee);
											response.put("payloadLength", blockData.payloadLength);
											response.put("version", blockData.version);
											response.put("baseTarget", convert(blockData.baseTarget));
											if (blockData.previousBlock != 0) {
												
												response.put("previousBlock", convert(blockData.previousBlock));
												
											}
											if (blockData.nextBlock != 0) {
												
												response.put("nextBlock", convert(blockData.nextBlock));
												
											}
											response.put("payloadHash", convert(blockData.payloadHash));
											response.put("generationSignature", convert(blockData.generationSignature));
											if (blockData.version > 1) {
												
												response.put("previousBlockHash", convert(blockData.previousBlockHash));
												
											}
											response.put("blockSignature", convert(blockData.blockSignature));
											JSONArray transactions = new JSONArray();
											for (int i = 0; i < blockData.numberOfTransactions; i++) {
												
												transactions.add(convert(blockData.transactions[i]));
												
											}
											response.put("transactions", transactions);
											
										}
										
									} catch (Exception e) {
										
										response.put("errorCode", 4);
										response.put("errorDescription", "Incorrect \"block\"");
										
									}
									
								}
								
							}
							break;

                        //TODO: cache
						case "getConstants":
							{
								
								JSONArray transactionTypes = new JSONArray();
								JSONObject transactionType = new JSONObject();
								transactionType.put("value", Transaction.TYPE_PAYMENT);
								transactionType.put("description", "Payment");
								JSONArray subtypes = new JSONArray();
								JSONObject subtype = new JSONObject();
								subtype.put("value", Transaction.SUBTYPE_PAYMENT_ORDINARY_PAYMENT);
								subtype.put("description", "Ordinary payment");
								subtypes.add(subtype);
								transactionType.put("subtypes", subtypes);
								transactionTypes.add(transactionType);
								transactionType = new JSONObject();
								transactionType.put("value", Transaction.TYPE_MESSAGING);
								transactionType.put("description", "Messaging");
								subtypes = new JSONArray();
								subtype = new JSONObject();
								subtype.put("value", Transaction.SUBTYPE_MESSAGING_ARBITRARY_MESSAGE);
								subtype.put("description", "Arbitrary message");
								subtypes.add(subtype);
								subtype = new JSONObject();
								subtype.put("value", Transaction.SUBTYPE_MESSAGING_ALIAS_ASSIGNMENT);
								subtype.put("description", "Alias assignment");
								subtypes.add(subtype);
								transactionType.put("subtypes", subtypes);
								transactionTypes.add(transactionType);
								transactionType = new JSONObject();
								transactionType.put("value", Transaction.TYPE_COLORED_COINS);
								transactionType.put("description", "Colored coins");
								subtypes = new JSONArray();
								subtype = new JSONObject();
								subtype.put("value", Transaction.SUBTYPE_COLORED_COINS_ASSET_ISSUANCE);
								subtype.put("description", "Asset issuance");
								subtypes.add(subtype);
								subtype = new JSONObject();
								subtype.put("value", Transaction.SUBTYPE_COLORED_COINS_ASSET_TRANSFER);
								subtype.put("description", "Asset transfer");
								subtypes.add(subtype);
								subtype = new JSONObject();
								subtype.put("value", Transaction.SUBTYPE_COLORED_COINS_ASK_ORDER_PLACEMENT);
								subtype.put("description", "Ask order placement");
								subtypes.add(subtype);
								subtype = new JSONObject();
								subtype.put("value", Transaction.SUBTYPE_COLORED_COINS_BID_ORDER_PLACEMENT);
								subtype.put("description", "Bid order placement");
								subtypes.add(subtype);
								subtype = new JSONObject();
								subtype.put("value", Transaction.SUBTYPE_COLORED_COINS_ASK_ORDER_CANCELLATION);
								subtype.put("description", "Ask order cancellation");
								subtypes.add(subtype);
								subtype = new JSONObject();
								subtype.put("value", Transaction.SUBTYPE_COLORED_COINS_BID_ORDER_CANCELLATION);
								subtype.put("description", "Bid order cancellation");
								subtypes.add(subtype);
								transactionType.put("subtypes", subtypes);
								transactionTypes.add(transactionType);
								response.put("transactionTypes", transactionTypes);
								
								JSONArray peerStates = new JSONArray();
								JSONObject peerState = new JSONObject();
								peerState.put("value", 0);
								peerState.put("description", "Non-connected");
								peerStates.add(peerState);
								peerState = new JSONObject();
								peerState.put("value", 1);
								peerState.put("description", "Connected");
								peerStates.add(peerState);
								peerState = new JSONObject();
								peerState.put("value", 2);
								peerState.put("description", "Disconnected");
								peerStates.add(peerState);
								response.put("peerStates", peerStates);
								
							}
							break;
							
						case "getMyInfo":
							{
								
								response.put("host", req.getRemoteHost());
								response.put("address", req.getRemoteAddr());
								
							}
							break;
							
						case "getPeer":
							{
								
								String peer = req.getParameter("peer");
								if (peer == null) {
									
									response.put("errorCode", 3);
									response.put("errorDescription", "\"peer\" not specified");
									
								} else {
									
									Peer peerData = peers.get(peer);
									if (peerData == null) {
										
										response.put("errorCode", 5);
										response.put("errorDescription", "Unknown peer");
										
									} else {
										
										response.put("state", peerData.state);
										response.put("announcedAddress", peerData.announcedAddress);
										if (peerData.hallmark != null) {
											
											response.put("hallmark", peerData.hallmark);
											
										}
										response.put("weight", peerData.getWeight());
										response.put("downloadedVolume", peerData.downloadedVolume);
										response.put("uploadedVolume", peerData.uploadedVolume);
										response.put("application", peerData.application);
										response.put("version", peerData.version);
										response.put("platform", peerData.platform);
										
									}
									
								}
								
							}
							break;
							
						case "getPeers":
							{
								
								JSONArray peers = new JSONArray();
                                peers.addAll(Nxt.peers.keySet());
								response.put("peers", peers);
								
							}
							break;
							
						case "getState":
							{
								
								response.put("version", VERSION);
								response.put("time", getEpochTime(System.currentTimeMillis()));
								response.put("lastBlock", convert(lastBlock));
								response.put("numberOfBlocks", blocks.size());
								response.put("numberOfTransactions", transactions.size());
								response.put("numberOfAccounts", accounts.size());
								response.put("numberOfAssets", assets.size());
								response.put("numberOfOrders", askOrders.size() + bidOrders.size());
								response.put("numberOfAliases", aliases.size());
								response.put("numberOfPeers", peers.size());
								response.put("numberOfUsers", users.size());
								response.put("lastBlockchainFeeder", lastBlockchainFeeder == null ? null : lastBlockchainFeeder.announcedAddress);
								response.put("availableProcessors", Runtime.getRuntime().availableProcessors());
								response.put("maxMemory", Runtime.getRuntime().maxMemory());
								response.put("totalMemory", Runtime.getRuntime().totalMemory());
								response.put("freeMemory", Runtime.getRuntime().freeMemory());
								
							}
							break;
							
						case "getTime":
							{
								
								response.put("time", getEpochTime(System.currentTimeMillis()));
								
							}
							break;
							
						case "getTransaction":
							{
								
								String transaction = req.getParameter("transaction");
								if (transaction == null) {
									
									response.put("errorCode", 3);
									response.put("errorDescription", "\"transaction\" not specified");
									
								} else {
									
									try {
										
										long transactionId = (new BigInteger(transaction)).longValue();
										Transaction transactionData = transactions.get(transactionId);
										if (transactionData == null) {
											
											transactionData = unconfirmedTransactions.get(transactionId);
											if (transactionData == null) {
												
												response.put("errorCode", 5);
												response.put("errorDescription", "Unknown transaction");
												
											} else {
												
												response = transactionData.getJSONObject();
												response.put("sender", convert(Account.getId(transactionData.senderPublicKey)));
												
											}
											
										} else {
											
											response = transactionData.getJSONObject();
											
											response.put("sender", convert(Account.getId(transactionData.senderPublicKey)));
											Block block = blocks.get(transactionData.block);
											response.put("block", convert(block.getId()));
											response.put("confirmations", Block.getLastBlock().height - block.height + 1);
											
										}
										
									} catch (Exception e) {
										
										response.put("errorCode", 4);
										response.put("errorDescription", "Incorrect \"transaction\"");
										
									}
									
								}
								
							}
							break;
							
						case "getTransactionBytes":
							{
								
								String transaction = req.getParameter("transaction");
								if (transaction == null) {
									
									response.put("errorCode", 3);
									response.put("errorDescription", "\"transaction\" not specified");
									
								} else {
									
									try {
										
										long transactionId = (new BigInteger(transaction)).longValue();
										Transaction transactionData = transactions.get(transactionId);
										if (transactionData == null) {
											
											transactionData = unconfirmedTransactions.get(transactionId);
											if (transactionData == null) {
												
												response.put("errorCode", 5);
												response.put("errorDescription", "Unknown transaction");
												
											} else {
												
												response.put("bytes", convert(transactionData.getBytes()));
												
											}
											
										} else {
											
											response.put("bytes", convert(transactionData.getBytes()));
											Block block = blocks.get(transactionData.block);
											response.put("confirmations", Block.getLastBlock().height - block.height + 1);
											
										}
										
									} catch (Exception e) {
										
										response.put("errorCode", 4);
										response.put("errorDescription", "Incorrect \"transaction\"");
										
									}
									
								}
								
							}
							break;
							
						case "getUnconfirmedTransactionIds":
							{
								
								JSONArray transactionIds = new JSONArray();
								for (Long transactionId : unconfirmedTransactions.keySet()) {
									
									transactionIds.add(convert(transactionId));
									
								}
								response.put("unconfirmedTransactionIds", transactionIds);
								
							}
							break;
							
						/*case "issueAsset":
							{
								
								String secretPhrase = req.getParameter("secretPhrase");
								String name = req.getParameter("name");
								String description = req.getParameter("description");
								String quantityValue = req.getParameter("quantity");
								String feeValue = req.getParameter("fee");
								if (secretPhrase == null) {
									
									response.put("errorCode", 3);
									response.put("errorDescription", "\"secretPhrase\" not specified");
									
								} else if (name == null) {
									
									response.put("errorCode", 3);
									response.put("errorDescription", "\"name\" not specified");
									
								} else if (quantityValue == null) {
									
									response.put("errorCode", 3);
									response.put("errorDescription", "\"quantity\" not specified");
									
								} else if (feeValue == null) {
									
									response.put("errorCode", 3);
									response.put("errorDescription", "\"fee\" not specified");
									
								} else {
									
									name = name.trim();
									if (name.length() < 3 || name.length() > 10) {
										
										response.put("errorCode", 4);
										response.put("errorDescription", "Incorrect \"name\" (length must be in [3..10] range)");
										
									} else {
										
										String normalizedName = name.toLowerCase();
										int i;
										for (i = 0; i < normalizedName.length(); i++) {
											
											if (alphabet.indexOf(normalizedName.charAt(i)) < 0) {
												
												break;
												
											}
											
										}
										if (i != normalizedName.length()) {
											
											response.put("errorCode", 4);
											response.put("errorDescription", "Incorrect \"name\" (must contain only digits and latin letters)");
											
										} else if (assetNameToIdMappings.get(normalizedName) != null) {
											
											response.put("errorCode", 8);
											response.put("errorDescription", "\"" + name + "\" is already used");
											
										} else {
											
											if (description != null && description.length() > 1000) {
												
												response.put("errorCode", 4);
												response.put("errorDescription", "Incorrect \"description\" (length must be not longer than 1000 characters)");
												
											} else {
												
												try {
													
													int quantity = Integer.parseInt(quantityValue);
													if (quantity <= 0 || quantity > 1000000000) {
														
														response.put("errorCode", 4);
														response.put("errorDescription", "Incorrect \"quantity\" (must be in [1..1'000'000'000] range)");
														
													} else {
														
														try {
															
															int fee = Integer.parseInt(feeValue);
															if (fee < Transaction.ASSET_ISSUANCE_FEE) {
																
																response.put("errorCode", 4);
																response.put("errorDescription", "Incorrect \"fee\" (must be not less than 1'000)");
																
															} else {
																
																byte[] publicKey = Crypto.getPublicKey(secretPhrase);
																Account account = accounts.get(Account.getId(publicKey));
																if (account == null) {
																	
																	response.put("errorCode", 6);
																	response.put("errorDescription", "Not enough funds");
																	
																} else {
																	
																	if (fee * 100L > account.unconfirmedBalance) {
																		
																		response.put("errorCode", 6);
																		response.put("errorDescription", "Not enough funds");
																		
																	} else {
																		
																		int timestamp = getEpochTime(System.currentTimeMillis());
																		
																		Transaction transaction = new Transaction(Transaction.TYPE_COLORED_COINS, Transaction.SUBTYPE_COLORED_COINS_ASSET_ISSUANCE, timestamp, (short)1440, publicKey, CREATOR_ID, 0, fee, 0, new byte[64]);
																		transaction.attachment = new Transaction.ColoredCoinsAssetIssuanceAttachment(name, description, quantity);
																		transaction.sign(secretPhrase);
																		
																		JSONObject peerRequest = new JSONObject();
																		peerRequest.put("requestType", "processTransactions");
																		JSONArray transactionsData = new JSONArray();
																		transactionsData.add(transaction.getJSONObject());
																		peerRequest.put("transactions", transactionsData);
																		
																		Peer.sendToAllPeers(peerRequest);
																		
																		response.put("transaction", convert(transaction.getId()));
																		
																	}
																	
																}
																
															}
															
														} catch (Exception e) {
															
															response.put("errorCode", 4);
															response.put("errorDescription", "Incorrect \"fee\""+e.toString());
															
														}
														
													}
													
												} catch (Exception e) {
													
													response.put("errorCode", 4);
													response.put("errorDescription", "Incorrect \"quantity\"");
													
												}
												
											}
											
										}
										
									}
									
								}
								
							}
							break;
							
						case "cancelAskOrder":
							{
								
								String secretPhrase = req.getParameter("secretPhrase");
								String orderValue = req.getParameter("order");
								String feeValue = req.getParameter("fee");
								String deadlineValue = req.getParameter("deadline");
								String referencedTransactionValue = req.getParameter("referencedTransaction");
								if (secretPhrase == null) {
									
									response.put("errorCode", 3);
									response.put("errorDescription", "\"secretPhrase\" not specified");
									
								} else if (orderValue == null) {
									
									response.put("errorCode", 3);
									response.put("errorDescription", "\"order\" not specified");
									
								} else if (feeValue == null) {
									
									response.put("errorCode", 3);
									response.put("errorDescription", "\"fee\" not specified");
									
								} else if (deadlineValue == null) {
									
									response.put("errorCode", 3);
									response.put("errorDescription", "\"deadline\" not specified");
									
								} else {
									
									try {
										
										long order = (new BigInteger(orderValue)).longValue();
										
										try {
											
											int fee = Integer.parseInt(feeValue);
											if (fee <= 0 || fee >= 1000000000) {
												
												throw new Exception();
												
											}
											
											try {
												
												short deadline = Short.parseShort(deadlineValue);
												if (deadline < 1) {
													
													throw new Exception();
													
												}
												
												long referencedTransaction = referencedTransactionValue == null ? 0 : (new BigInteger(referencedTransactionValue)).longValue();
												
												byte[] publicKey = Crypto.getPublicKey(secretPhrase);
												long accountId = Account.getId(publicKey);
												
												AskOrder orderData = askOrders.get(order);
												if (orderData == null || orderData.account.id != accountId) {
													
													response.put("errorCode", 5);
													response.put("errorDescription", "Unknown order");
													
												} else {
													
													Account account = accounts.get(accountId);
													if (account == null) {
														
														response.put("errorCode", 6);
														response.put("errorDescription", "Not enough funds");
														
													} else {
														
														if (fee * 100L > account.unconfirmedBalance) {
															
															response.put("errorCode", 6);
															response.put("errorDescription", "Not enough funds");
															
														} else {
															
															int timestamp = getEpochTime(System.currentTimeMillis());
															
															Transaction transaction = new Transaction(Transaction.TYPE_COLORED_COINS, Transaction.SUBTYPE_COLORED_COINS_ASK_ORDER_CANCELLATION, timestamp, deadline, publicKey, CREATOR_ID, 0, fee, referencedTransaction, new byte[64]);
															transaction.attachment = new Transaction.ColoredCoinsAskOrderCancellationAttachment(order);
															transaction.sign(secretPhrase);
															
															JSONObject peerRequest = new JSONObject();
															peerRequest.put("requestType", "processTransactions");
															JSONArray transactionsData = new JSONArray();
															transactionsData.add(transaction.getJSONObject());
															peerRequest.put("transactions", transactionsData);
															
															Peer.sendToAllPeers(peerRequest);
															
															response.put("transaction", convert(transaction.getId()));
															
														}
														
													}
													
												}
												
											} catch (Exception e) {
												
												response.put("errorCode", 4);
												response.put("errorDescription", "Incorrect \"deadline\"");
												
											}
											
										} catch (Exception e) {
											
											response.put("errorCode", 4);
											response.put("errorDescription", "Incorrect \"fee\"");
											
										}
										
									} catch (Exception e) {
										
										response.put("errorCode", 4);
										response.put("errorDescription", "Incorrect \"order\"");
										
									}
									
								}
								
							}
							break;
							
						case "cancelBidOrder":
							{
								
								String secretPhrase = req.getParameter("secretPhrase");
								String orderValue = req.getParameter("order");
								String feeValue = req.getParameter("fee");
								String deadlineValue = req.getParameter("deadline");
								String referencedTransactionValue = req.getParameter("referencedTransaction");
								if (secretPhrase == null) {
									
									response.put("errorCode", 3);
									response.put("errorDescription", "\"secretPhrase\" not specified");
									
								} else if (orderValue == null) {
									
									response.put("errorCode", 3);
									response.put("errorDescription", "\"order\" not specified");
									
								} else if (feeValue == null) {
									
									response.put("errorCode", 3);
									response.put("errorDescription", "\"fee\" not specified");
									
								} else if (deadlineValue == null) {
									
									response.put("errorCode", 3);
									response.put("errorDescription", "\"deadline\" not specified");
									
								} else {
									
									try {
										
										long order = (new BigInteger(orderValue)).longValue();
										
										try {
											
											int fee = Integer.parseInt(feeValue);
											if (fee <= 0 || fee >= 1000000000) {
												
												throw new Exception();
												
											}
											
											try {
												
												short deadline = Short.parseShort(deadlineValue);
												if (deadline < 1) {
													
													throw new Exception();
													
												}
												
												long referencedTransaction = referencedTransactionValue == null ? 0 : (new BigInteger(referencedTransactionValue)).longValue();
												
												byte[] publicKey = Crypto.getPublicKey(secretPhrase);
												long accountId = Account.getId(publicKey);
												
												BidOrder orderData = bidOrders.get(order);
												if (orderData == null || orderData.account.id != accountId) {
													
													response.put("errorCode", 5);
													response.put("errorDescription", "Unknown order");
													
												} else {
													
													Account account = accounts.get(accountId);
													if (account == null) {
														
														response.put("errorCode", 6);
														response.put("errorDescription", "Not enough funds");
														
													} else {
														
														if (fee * 100L > account.unconfirmedBalance) {
															
															response.put("errorCode", 6);
															response.put("errorDescription", "Not enough funds");
															
														} else {
															
															int timestamp = getEpochTime(System.currentTimeMillis());
															
															Transaction transaction = new Transaction(Transaction.TYPE_COLORED_COINS, Transaction.SUBTYPE_COLORED_COINS_BID_ORDER_CANCELLATION, timestamp, deadline, publicKey, CREATOR_ID, 0, fee, referencedTransaction, new byte[64]);
															transaction.attachment = new Transaction.ColoredCoinsBidOrderCancellationAttachment(order);
															transaction.sign(secretPhrase);
															
															JSONObject peerRequest = new JSONObject();
															peerRequest.put("requestType", "processTransactions");
															JSONArray transactionsData = new JSONArray();
															transactionsData.add(transaction.getJSONObject());
															peerRequest.put("transactions", transactionsData);
															
															Peer.sendToAllPeers(peerRequest);
															
															response.put("transaction", convert(transaction.getId()));
															
														}
														
													}
													
												}
												
											} catch (Exception e) {
												
												response.put("errorCode", 4);
												response.put("errorDescription", "Incorrect \"deadline\"");
												
											}
											
										} catch (Exception e) {
											
											response.put("errorCode", 4);
											response.put("errorDescription", "Incorrect \"fee\"");
											
										}
										
									} catch (Exception e) {
										
										response.put("errorCode", 4);
										response.put("errorDescription", "Incorrect \"order\"");
										
									}
									
								}
								
							}
							break;
							
						case "getAsset":
							{
								
								String asset = req.getParameter("asset");
								if (asset == null) {
									
									response.put("errorCode", 3);
									response.put("errorDescription", "\"asset\" not specified");
									
								} else {
									
									try {
										
										long assetId = (new BigInteger(asset)).longValue();
										Asset assetData = assets.get(assetId);
										if (assetData == null) {
											
											response.put("errorCode", 5);
											response.put("errorDescription", "Unknown asset");
											
										} else {
											
											response.put("account", convert(assetData.accountId));
											response.put("name", assetData.name);
											if (assetData.description.length() > 0) {
												
												response.put("description", assetData.description);
												
											}
											response.put("quantity", assetData.quantity);
											
										}
										
									} catch (Exception e) {
										
										response.put("errorCode", 4);
										response.put("errorDescription", "Incorrect \"asset\"");
										
									}
									
								}
								
							}
							break;
							
						case "getAssetIds":
							{
								
								JSONArray assetIds = new JSONArray();
								for (Long assetId : assets.keySet()) {
									
									assetIds.add(convert(assetId));
									
								}
								response.put("assetIds", assetIds);
								
							}
							break;
							
						case "transferAsset":
							{
								
								String secretPhrase = req.getParameter("secretPhrase");
								String recipientValue = req.getParameter("recipient");
								String assetValue = req.getParameter("asset");
								String quantityValue = req.getParameter("quantity");
								String feeValue = req.getParameter("fee");
								String deadlineValue = req.getParameter("deadline");
								String referencedTransactionValue = req.getParameter("referencedTransaction");
								if (secretPhrase == null) {
									
									response.put("errorCode", 3);
									response.put("errorDescription", "\"secretPhrase\" not specified");
									
								} else if (recipientValue == null) {
									
									response.put("errorCode", 3);
									response.put("errorDescription", "\"recipient\" not specified");
									
								} else if (assetValue == null) {
									
									response.put("errorCode", 3);
									response.put("errorDescription", "\"asset\" not specified");
									
								} else if (quantityValue == null) {
									
									response.put("errorCode", 3);
									response.put("errorDescription", "\"quantity\" not specified");
									
								} else if (feeValue == null) {
									
									response.put("errorCode", 3);
									response.put("errorDescription", "\"fee\" not specified");
									
								} else if (deadlineValue == null) {
									
									response.put("errorCode", 3);
									response.put("errorDescription", "\"deadline\" not specified");
									
								} else {
									
									try {
										
										long recipient = (new BigInteger(recipientValue)).longValue();
										
										try {
											
											long asset = (new BigInteger(assetValue)).longValue();;
											
											try {
												
												int quantity = Integer.parseInt(quantityValue);
												if (quantity <= 0 || quantity >= 1000000000) {
													
													throw new Exception();
													
												}
												
												try {
													
													int fee = Integer.parseInt(feeValue);
													if (fee <= 0 || fee >= 1000000000) {
														
														throw new Exception();
														
													}
													
													try {
														
														short deadline = Short.parseShort(deadlineValue);
														if (deadline < 1) {
															
															throw new Exception();
															
														}
														
														long referencedTransaction = referencedTransactionValue == null ? 0 : (new BigInteger(referencedTransactionValue)).longValue();
														
														byte[] publicKey = Crypto.getPublicKey(secretPhrase);
														
														Account account = accounts.get(Account.getId(publicKey));
														if (account == null) {
															
															response.put("errorCode", 6);
															response.put("errorDescription", "Not enough funds");
															
														} else {
															
															if (fee * 100L > account.unconfirmedBalance) {
																
																response.put("errorCode", 6);
																response.put("errorDescription", "Not enough funds");
																
															} else {
																
																Integer assetBalance = account.unconfirmedAssetBalances.get(asset);
																if (assetBalance == null || quantity > assetBalance) {
																	
																	response.put("errorCode", 6);
																	response.put("errorDescription", "Not enough funds");
																	
																} else {
																	
																	int timestamp = getEpochTime(System.currentTimeMillis());
																	
																	Transaction transaction = new Transaction(Transaction.TYPE_COLORED_COINS, Transaction.SUBTYPE_COLORED_COINS_ASSET_TRANSFER, timestamp, deadline, publicKey, recipient, 0, fee, referencedTransaction, new byte[64]);
																	transaction.attachment = new Transaction.ColoredCoinsAssetTransferAttachment(asset, quantity);
																	transaction.sign(secretPhrase);
																	
																	JSONObject peerRequest = new JSONObject();
																	peerRequest.put("requestType", "processTransactions");
																	JSONArray transactionsData = new JSONArray();
																	transactionsData.add(transaction.getJSONObject());
																	peerRequest.put("transactions", transactionsData);
																	
																	Peer.sendToAllPeers(peerRequest);
																	
																	response.put("transaction", convert(transaction.getId()));
																	
																}
																
															}
															
														}
														
													} catch (Exception e) {
														
														response.put("errorCode", 4);
														response.put("errorDescription", "Incorrect \"deadline\""+e.toString());
														
													}
													
												} catch (Exception e) {
													
													response.put("errorCode", 4);
													response.put("errorDescription", "Incorrect \"fee\"");
													
												}
												
											} catch (Exception e) {
												
												response.put("errorCode", 4);
												response.put("errorDescription", "Incorrect \"quantity\"");
												
											}
											
										} catch (Exception e) {
											
											response.put("errorCode", 4);
											response.put("errorDescription", "Incorrect \"asset\"");
											
										}
										
									} catch (Exception e) {
										
										response.put("errorCode", 4);
										response.put("errorDescription", "Incorrect \"recipient\"");
										
									}
									
								}
								
							}
							break;
							
						case "placeAskOrder":
							{
								
								String secretPhrase = req.getParameter("secretPhrase");
								String assetValue = req.getParameter("asset");
								String quantityValue = req.getParameter("quantity");
								String priceValue = req.getParameter("price");
								String feeValue = req.getParameter("fee");
								String deadlineValue = req.getParameter("deadline");
								String referencedTransactionValue = req.getParameter("referencedTransaction");
								if (secretPhrase == null) {
									
									response.put("errorCode", 3);
									response.put("errorDescription", "\"secretPhrase\" not specified");
									
								} else if (assetValue == null) {
									
									response.put("errorCode", 3);
									response.put("errorDescription", "\"asset\" not specified");
									
								} else if (quantityValue == null) {
									
									response.put("errorCode", 3);
									response.put("errorDescription", "\"quantity\" not specified");
									
								} else if (priceValue == null) {
									
									response.put("errorCode", 3);
									response.put("errorDescription", "\"price\" not specified");
									
								} else if (feeValue == null) {
									
									response.put("errorCode", 3);
									response.put("errorDescription", "\"fee\" not specified");
									
								} else if (deadlineValue == null) {
									
									response.put("errorCode", 3);
									response.put("errorDescription", "\"deadline\" not specified");
									
								} else {
									
									try {
										
										long price = Long.parseLong(priceValue);
										if (price <= 0 || price > 1000000000 * 100L) {
											
											throw new Exception();
											
										}
										
										try {
											
											long asset = (new BigInteger(assetValue)).longValue();
											
											try {
												
												int quantity = Integer.parseInt(quantityValue);
												if (quantity <= 0 || quantity >= 1000000000) {
													
													throw new Exception();
													
												}
												
												try {
													
													int fee = Integer.parseInt(feeValue);
													if (fee <= 0 || fee >= 1000000000) {
														
														throw new Exception();
														
													}
													
													try {
														
														short deadline = Short.parseShort(deadlineValue);
														if (deadline < 1) {
															
															throw new Exception();
															
														}
														
														long referencedTransaction = referencedTransactionValue == null ? 0 : (new BigInteger(referencedTransactionValue)).longValue();
														
														byte[] publicKey = Crypto.getPublicKey(secretPhrase);
														
														Account account = accounts.get(Account.getId(publicKey));
														if (account == null) {
															
															response.put("errorCode", 6);
															response.put("errorDescription", "Not enough funds");
															
														} else {
															
															if (fee * 100L > account.unconfirmedBalance) {
																
																response.put("errorCode", 6);
																response.put("errorDescription", "Not enough funds");
																
															} else {
																
																Integer assetBalance = account.unconfirmedAssetBalances.get(asset);
																if (assetBalance == null || quantity > assetBalance) {
																	
																	response.put("errorCode", 6);
																	response.put("errorDescription", "Not enough funds");
																	
																} else {
																	
																	int timestamp = getEpochTime(System.currentTimeMillis());
																	
																	Transaction transaction = new Transaction(Transaction.TYPE_COLORED_COINS, Transaction.SUBTYPE_COLORED_COINS_ASK_ORDER_PLACEMENT, timestamp, deadline, publicKey, CREATOR_ID, 0, fee, referencedTransaction, new byte[64]);
																	transaction.attachment = new Transaction.ColoredCoinsAskOrderPlacementAttachment(asset, quantity, price);
																	transaction.sign(secretPhrase);
																	
																	JSONObject peerRequest = new JSONObject();
																	peerRequest.put("requestType", "processTransactions");
																	JSONArray transactionsData = new JSONArray();
																	transactionsData.add(transaction.getJSONObject());
																	peerRequest.put("transactions", transactionsData);
																	
																	Peer.sendToAllPeers(peerRequest);
																	
																	response.put("transaction", convert(transaction.getId()));
																	
																}
																
															}
															
														}
														
													} catch (Exception e) {
														
														response.put("errorCode", 4);
														response.put("errorDescription", "Incorrect \"deadline\"");
														
													}
													
												} catch (Exception e) {
													
													response.put("errorCode", 4);
													response.put("errorDescription", "Incorrect \"fee\"");
													
												}
												
											} catch (Exception e) {
												
												response.put("errorCode", 4);
												response.put("errorDescription", "Incorrect \"quantity\"");
												
											}
											
										} catch (Exception e) {
											
											response.put("errorCode", 4);
											response.put("errorDescription", "Incorrect \"asset\"");
											
										}
										
									} catch (Exception e) {
										
										response.put("errorCode", 4);
										response.put("errorDescription", "Incorrect \"price\"");
										
									}
									
								}
								
							}
							break;
							
						case "placeBidOrder":
							{
								
								String secretPhrase = req.getParameter("secretPhrase");
								String assetValue = req.getParameter("asset");
								String quantityValue = req.getParameter("quantity");
								String priceValue = req.getParameter("price");
								String feeValue = req.getParameter("fee");
								String deadlineValue = req.getParameter("deadline");
								String referencedTransactionValue = req.getParameter("referencedTransaction");
								if (secretPhrase == null) {
									
									response.put("errorCode", 3);
									response.put("errorDescription", "\"secretPhrase\" not specified");
									
								} else if (assetValue == null) {
									
									response.put("errorCode", 3);
									response.put("errorDescription", "\"asset\" not specified");
									
								} else if (quantityValue == null) {
									
									response.put("errorCode", 3);
									response.put("errorDescription", "\"quantity\" not specified");
									
								} else if (priceValue == null) {
									
									response.put("errorCode", 3);
									response.put("errorDescription", "\"price\" not specified");
									
								} else if (feeValue == null) {
									
									response.put("errorCode", 3);
									response.put("errorDescription", "\"fee\" not specified");
									
								} else if (deadlineValue == null) {
									
									response.put("errorCode", 3);
									response.put("errorDescription", "\"deadline\" not specified");
									
								} else {
									
									try {
										
										long price = Long.parseLong(priceValue);
										if (price <= 0 || price > 1000000000 * 100L) {
											
											throw new Exception();
											
										}
										
										try {
											
											long asset = (new BigInteger(assetValue)).longValue();
											
											try {
												
												int quantity = Integer.parseInt(quantityValue);
												if (quantity <= 0 || quantity >= 1000000000) {
													
													throw new Exception();
													
												}
												
												try {
													
													int fee = Integer.parseInt(feeValue);
													if (fee <= 0 || fee >= 1000000000) {
														
														throw new Exception();
														
													}
													
													try {
														
														short deadline = Short.parseShort(deadlineValue);
														if (deadline < 1) {
															
															throw new Exception();
															
														}
														
														long referencedTransaction = referencedTransactionValue == null ? 0 : (new BigInteger(referencedTransactionValue)).longValue();
														
														byte[] publicKey = Crypto.getPublicKey(secretPhrase);
														
														Account account = accounts.get(Account.getId(publicKey));
														if (account == null) {
															
															response.put("errorCode", 6);
															response.put("errorDescription", "Not enough funds");
															
														} else {
															
															if (quantity * price + fee * 100L > account.unconfirmedBalance) {
																
																response.put("errorCode", 6);
																response.put("errorDescription", "Not enough funds");
																
															} else {
																
																int timestamp = getEpochTime(System.currentTimeMillis());
																
																Transaction transaction = new Transaction(Transaction.TYPE_COLORED_COINS, Transaction.SUBTYPE_COLORED_COINS_BID_ORDER_PLACEMENT, timestamp, deadline, publicKey, CREATOR_ID, 0, fee, referencedTransaction, new byte[64]);
																transaction.attachment = new Transaction.ColoredCoinsBidOrderPlacementAttachment(asset, quantity, price);
																transaction.sign(secretPhrase);
																
																JSONObject peerRequest = new JSONObject();
																peerRequest.put("requestType", "processTransactions");
																JSONArray transactionsData = new JSONArray();
																transactionsData.add(transaction.getJSONObject());
																peerRequest.put("transactions", transactionsData);
																
																Peer.sendToAllPeers(peerRequest);
																
																response.put("transaction", convert(transaction.getId()));
																
															}
															
														}
														
													} catch (Exception e) {
														
														response.put("errorCode", 4);
														response.put("errorDescription", "Incorrect \"deadline\"");
														
													}
													
												} catch (Exception e) {
													
													response.put("errorCode", 4);
													response.put("errorDescription", "Incorrect \"fee\"");
													
												}
												
											} catch (Exception e) {
												
												response.put("errorCode", 4);
												response.put("errorDescription", "Incorrect \"quantity\"");
												
											}
											
										} catch (Exception e) {
											
											response.put("errorCode", 4);
											response.put("errorDescription", "Incorrect \"asset\"");
											
										}
										
									} catch (Exception e) {
										
										response.put("errorCode", 4);
										response.put("errorDescription", "Incorrect \"price\"");
										
									}
									
								}
								
							}
							break;
							
						case "getAskOrder":
							{
								
								String order = req.getParameter("order");
								if (order == null) {
									
									response.put("errorCode", 3);
									response.put("errorDescription", "\"order\" not specified");
									
								} else {
									
									try {
										
										long orderId = (new BigInteger(order)).longValue();
										AskOrder orderData = askOrders.get(orderId);
										if (orderData == null) {
											
											response.put("errorCode", 5);
											response.put("errorDescription", "Unknown ask order");
											
										} else {
											
											response.put("account", convert(orderData.account.id));
											response.put("asset", convert(orderData.asset));
											response.put("quantity", orderData.quantity);
											response.put("price", orderData.price);
											
										}
										
									} catch (Exception e) {
										
										response.put("errorCode", 4);
										response.put("errorDescription", "Incorrect \"order\"");
										
									}
									
								}
								
							}
							break;
							
						case "getAskOrderIds":
							{
								
								JSONArray orderIds = new JSONArray();
								for (Long orderId : askOrders.keySet()) {
									
									orderIds.add(convert(orderId));
									
								}
								response.put("askOrderIds", orderIds);
								
							}
							break;
							
						case "getBidOrder":
							{
								
								String order = req.getParameter("order");
								if (order == null) {
									
									response.put("errorCode", 3);
									response.put("errorDescription", "\"order\" not specified");
									
								} else {
									
									try {
										
										long orderId = (new BigInteger(order)).longValue();
										BidOrder orderData = bidOrders.get(orderId);
										if (orderData == null) {
											
											response.put("errorCode", 5);
											response.put("errorDescription", "Unknown bid order");
											
										} else {
											
											response.put("account", convert(orderData.account.id));
											response.put("asset", convert(orderData.asset));
											response.put("quantity", orderData.quantity);
											response.put("price", orderData.price);
											
										}
										
									} catch (Exception e) {
										
										response.put("errorCode", 4);
										response.put("errorDescription", "Incorrect \"order\"");
										
									}
									
								}
								
							}
							break;
							
						case "getBidOrderIds":
							{
								
								JSONArray orderIds = new JSONArray();
								for (Long orderId : bidOrders.keySet()) {
									
									orderIds.add(convert(orderId));
									
								}
								response.put("bidOrderIds", orderIds);
								
							}
							break;*/
							
						case "listAccountAliases":
							{
								
								String account = req.getParameter("account");
								if (account == null) {
									
									response.put("errorCode", 3);
									response.put("errorDescription", "\"account\" not specified");
									
								} else {
									
									try {
										
										long accountId = (new BigInteger(account)).longValue();
										Account accountData = accounts.get(accountId);
										if (accountData == null) {
											
											response.put("errorCode", 5);
											response.put("errorDescription", "Unknown account");
											
										} else {
											
											JSONArray aliases = new JSONArray();
											for (Alias alias : Nxt.aliases.values()) {
												
												if (alias.account.id == accountId) {
													
													JSONObject aliasData = new JSONObject();
													aliasData.put("alias", alias.alias);
													aliasData.put("uri", alias.uri);
													aliases.add(aliasData);
													
												}
												
											}
											response.put("aliases", aliases);
											
										}
										
									} catch (Exception e) {
										
										response.put("errorCode", 4);
										response.put("errorDescription", "Incorrect \"account\"");
										
									}
									
								}
								
							}
							break;
							
						case "markHost":
							{
								
								String secretPhrase = req.getParameter("secretPhrase");
								String host = req.getParameter("host");
								String weightValue = req.getParameter("weight");
								String dateValue = req.getParameter("date");
								if (secretPhrase == null) {
									
									response.put("errorCode", 3);
									response.put("errorDescription", "\"secretPhrase\" not specified");
									
								} else if (host == null) {
									
									response.put("errorCode", 3);
									response.put("errorDescription", "\"host\" not specified");
									
								} else if (weightValue == null) {
									
									response.put("errorCode", 3);
									response.put("errorDescription", "\"weight\" not specified");
									
								} else if (dateValue == null) {
									
									response.put("errorCode", 3);
									response.put("errorDescription", "\"date\" not specified");
									
								} else {
									
									if (host.length() > 100) {
										
										response.put("errorCode", 4);
										response.put("errorDescription", "Incorrect \"host\" (the length exceeds 100 chars limit)");
										
									} else {
										
										try {
											
											int weight = Integer.parseInt(weightValue);
											if (weight <= 0 || weight > MAX_BALANCE) {
												
												throw new Exception();
												
											}
											
											try {
												
												int date = Integer.parseInt(dateValue.substring(0, 4)) * 10000 + Integer.parseInt(dateValue.substring(5, 7)) * 100 + Integer.parseInt(dateValue.substring(8, 10));
												
												byte[] publicKey = Crypto.getPublicKey(secretPhrase);
												byte[] hostBytes = host.getBytes("UTF-8");
												
												ByteBuffer buffer = ByteBuffer.allocate(32 + 2 + hostBytes.length + 4 + 4 + 1);
												buffer.order(ByteOrder.LITTLE_ENDIAN);
												buffer.put(publicKey);
												buffer.putShort((short)hostBytes.length);
												buffer.put(hostBytes);
												buffer.putInt(weight);
												buffer.putInt(date);
												
												byte[] data = buffer.array();
												byte[] signature;
												do {
													data[data.length - 1] = (byte)ThreadLocalRandom.current().nextInt();
													signature = Crypto.sign(data, secretPhrase);
												} while (!Crypto.verify(signature, data, publicKey));
												
												response.put("hallmark", convert(data) + convert(signature));
												
											} catch (Exception e) {
												
												response.put("errorCode", 4);
												response.put("errorDescription", "Incorrect \"date\"");
												
											}
											
										} catch (Exception e) {
											
											response.put("errorCode", 4);
											response.put("errorDescription", "Incorrect \"weight\"");
											
										}
										
									}
									
								}
								
							}
							break;
							
						case "sendMoney":
							{
								
								/*if (bootstrappingState < 0) {
									
									response.put("errorCode", 2);
									response.put("errorDescription", "Blockchain not up to date");
									
									break;
									
								}*/
								
								String secretPhrase = req.getParameter("secretPhrase");
								String recipientValue = req.getParameter("recipient");
								String amountValue = req.getParameter("amount");
								String feeValue = req.getParameter("fee");
								String deadlineValue = req.getParameter("deadline");
								String referencedTransactionValue = req.getParameter("referencedTransaction");
								if (secretPhrase == null) {
									
									response.put("errorCode", 3);
									response.put("errorDescription", "\"secretPhrase\" not specified");
									
								} else if (recipientValue == null) {
									
									response.put("errorCode", 3);
									response.put("errorDescription", "\"recipient\" not specified");
									
								} else if (amountValue == null) {
									
									response.put("errorCode", 3);
									response.put("errorDescription", "\"amount\" not specified");
									
								} else if (feeValue == null) {
									
									response.put("errorCode", 3);
									response.put("errorDescription", "\"fee\" not specified");
									
								} else if (deadlineValue == null) {
									
									response.put("errorCode", 3);
									response.put("errorDescription", "\"deadline\" not specified");
									
								} else {
									
									try {
										
										long recipient = (new BigInteger(recipientValue)).longValue();
										
										try {
											
											int amount = Integer.parseInt(amountValue);
											if (amount <= 0 || amount >= MAX_BALANCE) {
												
												throw new Exception();
												
											}
											
											try {
												
												int fee = Integer.parseInt(feeValue);
												if (fee <= 0 || fee >= MAX_BALANCE) {
													
													throw new Exception();
													
												}
												
												try {
													
													short deadline = Short.parseShort(deadlineValue);
													if (deadline < 1) {
														
														throw new Exception();
														
													}
													
													long referencedTransaction = referencedTransactionValue == null ? 0 : (new BigInteger(referencedTransactionValue)).longValue();
													
													byte[] publicKey = Crypto.getPublicKey(secretPhrase);
													
													Account account = accounts.get(Account.getId(publicKey));
													if (account == null) {
														
														response.put("errorCode", 6);
														response.put("errorDescription", "Not enough funds");
														
													} else {
														
														if ((amount + fee) * 100L > account.unconfirmedBalance) {
															
															response.put("errorCode", 6);
															response.put("errorDescription", "Not enough funds");
															
														} else {
															
															Transaction transaction = new Transaction(Transaction.TYPE_PAYMENT, Transaction.SUBTYPE_PAYMENT_ORDINARY_PAYMENT, getEpochTime(System.currentTimeMillis()), deadline, publicKey, recipient, amount, fee, referencedTransaction, new byte[64]);
															transaction.sign(secretPhrase);
															
															JSONObject peerRequest = new JSONObject();
															peerRequest.put("requestType", "processTransactions");
															JSONArray transactionsData = new JSONArray();
															transactionsData.add(transaction.getJSONObject());
															peerRequest.put("transactions", transactionsData);
															
															Peer.sendToAllPeers(peerRequest);
															
															response.put("transaction", convert(transaction.getId()));
															response.put("bytes", convert(transaction.getBytes()));
															
														}
														
													}
													
												} catch (Exception e) {
													
													response.put("errorCode", 4);
													response.put("errorDescription", "Incorrect \"deadline\"");
													
												}
												
											} catch (Exception e) {
												
												response.put("errorCode", 4);
												response.put("errorDescription", "Incorrect \"fee\"");
												
											}
											
										} catch (Exception e) {
											
											response.put("errorCode", 4);
											response.put("errorDescription", "Incorrect \"amount\"");
											
										}
										
									} catch (Exception e) {
										
										response.put("errorCode", 4);
										response.put("errorDescription", "Incorrect \"recipient\"");
										
									}
									
								}
								
							}
							break;
							
						default:
							{
								
								response.put("errorCode", 1);
								response.put("errorDescription", "Incorrect request");
								
							}
							
						}
						
					}
					
				}
				
				resp.setContentType("text/plain; charset=UTF-8");

                try (ServletOutputStream servletOutputStream = resp.getOutputStream()) {
                    servletOutputStream.write(response.toString().getBytes("UTF-8"));
                }

				return;
				
			} else { // userPasscode != null
				
				if (allowedUserHosts != null && !allowedUserHosts.contains(req.getRemoteHost())) {
					
					JSONObject response = new JSONObject();
					response.put("response", "denyAccess");
					JSONArray responses = new JSONArray();
					responses.add(response);
					JSONObject combinedResponse = new JSONObject();
					combinedResponse.put("responses", responses);
					
					resp.setContentType("text/plain; charset=UTF-8");

                    try (ServletOutputStream servletOutputStream = resp.getOutputStream()) {
                        servletOutputStream.write(combinedResponse.toString().getBytes("UTF-8"));
                    }
					return;
					
				}
				
				user = users.get(userPasscode);
				if (user == null) {
					
					user = new User();
					users.put(userPasscode, user);
					
				} else {
					
                    user.isInactive = false; // make sure to activate dormant user
					
				}
				
			}
			
			switch (req.getParameter("requestType")) {
			
			case "generateAuthorizationToken":
				{
					
					byte[] website = req.getParameter("website").trim().getBytes("UTF-8");
					byte[] data = new byte[website.length + 32 + 4];
					System.arraycopy(website, 0, data, 0, website.length);
					System.arraycopy(Crypto.getPublicKey(user.secretPhrase), 0, data, website.length, 32);
					int timestamp = getEpochTime(System.currentTimeMillis());
					data[website.length + 32] = (byte)timestamp;
					data[website.length + 32 + 1] = (byte)(timestamp >> 8);
					data[website.length + 32 + 2] = (byte)(timestamp >> 16);
					data[website.length + 32 + 3] = (byte)(timestamp >> 24);
					
					byte[] token = new byte[100];
					System.arraycopy(data, website.length, token, 0, 32 + 4);
					System.arraycopy(Crypto.sign(data, user.secretPhrase), 0, token, 32 + 4, 64);
					String tokenString = "";
					for (int ptr = 0; ptr < 100; ptr += 5) {
						
						long number = ((long)(token[ptr] & 0xFF)) | (((long)(token[ptr + 1] & 0xFF)) << 8) | (((long)(token[ptr + 2] & 0xFF)) << 16) | (((long)(token[ptr + 3] & 0xFF)) << 24) | (((long)(token[ptr + 4] & 0xFF)) << 32);
						if (number < 32) {
							
							tokenString += "0000000";
							
						} else if (number < 1024) {
							
							tokenString += "000000";
							
						} else if (number < 32768) {
							
							tokenString += "00000";
							
						} else if (number < 1048576) {
							
							tokenString += "0000";
							
						} else if (number < 33554432) {
							
							tokenString += "000";
							
						} else if (number < 1073741824) {
							
							tokenString += "00";
							
						} else if (number < 34359738368L) {
							
							tokenString += "0";
							
						}
						tokenString += Long.toString(number, 32);
						
					}
					
					JSONObject response = new JSONObject();
					response.put("response", "showAuthorizationToken");
					response.put("token", tokenString);
					
					user.pendingResponses.offer(response);
					
				}
				break;
				
			case "getInitialData":
				{
					
					JSONArray unconfirmedTransactions = new JSONArray();
					JSONArray activePeers = new JSONArray(), knownPeers = new JSONArray(), blacklistedPeers = new JSONArray();
					JSONArray recentBlocks = new JSONArray();

                    for (Transaction transaction : Nxt.unconfirmedTransactions.values()) {

                        JSONObject unconfirmedTransaction = new JSONObject();
                        unconfirmedTransaction.put("index", transaction.index);
                        unconfirmedTransaction.put("timestamp", transaction.timestamp);
                        unconfirmedTransaction.put("deadline", transaction.deadline);
                        unconfirmedTransaction.put("recipient", convert(transaction.recipient));
                        unconfirmedTransaction.put("amount", transaction.amount);
                        unconfirmedTransaction.put("fee", transaction.fee);
                        unconfirmedTransaction.put("sender", convert(Account.getId(transaction.senderPublicKey)));

                        unconfirmedTransactions.add(unconfirmedTransaction);

                    }

                    for (Map.Entry<String, Peer> peerEntry : peers.entrySet()) {

                        String address = peerEntry.getKey();
                        Peer peer = peerEntry.getValue();

                        if (peer.blacklistingTime > 0) {

                            JSONObject blacklistedPeer = new JSONObject();
                            blacklistedPeer.put("index", peer.index);
                            blacklistedPeer.put("announcedAddress", peer.announcedAddress.length() > 0 ? (peer.announcedAddress.length() > 30 ? (peer.announcedAddress.substring(0, 30) + "...") : peer.announcedAddress) : address);
                            for (String wellKnownPeer : wellKnownPeers) {

                                if (peer.announcedAddress.equals(wellKnownPeer)) {

                                    blacklistedPeer.put("wellKnown", true);

                                    break;

                                }

                            }

                            blacklistedPeers.add(blacklistedPeer);

                        } else if (peer.state == Peer.STATE_NONCONNECTED) {

                            if (peer.announcedAddress.length() > 0) {

                                JSONObject knownPeer = new JSONObject();
                                knownPeer.put("index", peer.index);
                                knownPeer.put("announcedAddress", peer.announcedAddress.length() > 30 ? (peer.announcedAddress.substring(0, 30) + "...") : peer.announcedAddress);
                                for (String wellKnownPeer : wellKnownPeers) {

                                    if (peer.announcedAddress.equals(wellKnownPeer)) {

                                        knownPeer.put("wellKnown", true);

                                        break;

                                    }

                                }

                                knownPeers.add(knownPeer);

                            }

                        } else {

                            JSONObject activePeer = new JSONObject();
                            activePeer.put("index", peer.index);
                            if (peer.state == peer.STATE_DISCONNECTED) {

                                activePeer.put("disconnected", true);

                            }
                            activePeer.put("address", address.length() > 30 ? (address.substring(0, 30) + "...") : address);
                            activePeer.put("announcedAddress", peer.announcedAddress.length() > 30 ? (peer.announcedAddress.substring(0, 30) + "...") : peer.announcedAddress);
                            activePeer.put("weight", peer.getWeight());
                            activePeer.put("downloaded", peer.downloadedVolume);
                            activePeer.put("uploaded", peer.uploadedVolume);
                            activePeer.put("software", (peer.application == null ? "?" : peer.application) + " (" + (peer.version == null ? "?" : peer.version) + ")" + " @ " + (peer.platform == null ? "?" : peer.platform));
                            for (String wellKnownPeer : wellKnownPeers) {

                                if (peer.announcedAddress.equals(wellKnownPeer)) {

                                    activePeer.put("wellKnown", true);

                                    break;

                                }

                            }

                            activePeers.add(activePeer);

                        }

                    }

                    long blockId = lastBlock;
                    int numberOfBlocks = 0;
                    while (numberOfBlocks < 60) {

                        numberOfBlocks++;

                        Block block = blocks.get(blockId);
                        JSONObject recentBlock = new JSONObject();
                        recentBlock.put("index", block.index);
                        recentBlock.put("timestamp", block.timestamp);
                        recentBlock.put("numberOfTransactions", block.numberOfTransactions);
                        recentBlock.put("totalAmount", block.totalAmount);
                        recentBlock.put("totalFee", block.totalFee);
                        recentBlock.put("payloadLength", block.payloadLength);
                        recentBlock.put("generator", convert(Account.getId(block.generatorPublicKey)));
                        recentBlock.put("height", block.height);
                        recentBlock.put("version", block.version);
                        recentBlock.put("block", convert(blockId));
                        recentBlock.put("baseTarget", BigInteger.valueOf(block.baseTarget).multiply(BigInteger.valueOf(100000)).divide(BigInteger.valueOf(initialBaseTarget)));

                        recentBlocks.add(recentBlock);

                        if (blockId == GENESIS_BLOCK_ID) {

                            break;

                        }

                        blockId = block.previousBlock;

                    }

					JSONObject response = new JSONObject();
					response.put("response", "processInitialData");
					response.put("version", VERSION);
					if (unconfirmedTransactions.size() > 0) {
						
						response.put("unconfirmedTransactions", unconfirmedTransactions);
						
					}
					if (activePeers.size() > 0) {
						
						response.put("activePeers", activePeers);
						
					}
					if (knownPeers.size() > 0) {
						
						response.put("knownPeers", knownPeers);
						
					}
					if (blacklistedPeers.size() > 0) {
						
						response.put("blacklistedPeers", blacklistedPeers);
						
					}
					if (recentBlocks.size() > 0) {
						
						response.put("recentBlocks", recentBlocks);
						
					}
					
					user.pendingResponses.offer(response);
					
				}
				break;
				
			case "getNewData":
				break;
				
			case "lockAccount":
				{
					
					user.deinitializeKeyPair();
					
					JSONObject response = new JSONObject();
					response.put("response", "lockAccount");
					
					user.pendingResponses.offer(response);
					
				}
				break;
				
			case "removeActivePeer":
				{
					
					if (allowedUserHosts == null && !InetAddress.getByName(req.getRemoteAddr()).isLoopbackAddress()) {
						
						JSONObject response = new JSONObject();
						response.put("response", "showMessage");
						response.put("message", "This operation is allowed to local host users only!");
						
						user.pendingResponses.offer(response);
						
					} else {
						
						int index = Integer.parseInt(req.getParameter("peer"));
						for (Peer peer : peers.values()) {
							
							if (peer.index == index) {
								
								if (peer.blacklistingTime == 0 && peer.state != Peer.STATE_NONCONNECTED) {
									
									peer.deactivate();
									
								}
								
								break;
								
							}
							
						}
						
					}
					
				}
				break;
				
			case "removeBlacklistedPeer":
				{
					
					if (allowedUserHosts == null && !InetAddress.getByName(req.getRemoteAddr()).isLoopbackAddress()) {
						
						JSONObject response = new JSONObject();
						response.put("response", "showMessage");
						response.put("message", "This operation is allowed to local host users only!");
						
						user.pendingResponses.offer(response);
						
					} else {
						
						int index = Integer.parseInt(req.getParameter("peer"));
						for (Peer peer : peers.values()) {
							
							if (peer.index == index) {
								
								if (peer.blacklistingTime > 0) {
									
									peer.removeBlacklistedStatus();
									
								}
								
								break;
								
							}
							
						}
						
					}
					
				}
				break;
				
			case "removeKnownPeer":
				{
					
					if (allowedUserHosts == null && !InetAddress.getByName(req.getRemoteAddr()).isLoopbackAddress()) {
						
						JSONObject response = new JSONObject();
						response.put("response", "showMessage");
						response.put("message", "This operation is allowed to local host users only!");
						
						user.pendingResponses.offer(response);
						
					} else {
						
						int index = Integer.parseInt(req.getParameter("peer"));
						for (Peer peer : peers.values()) {
							
							if (peer.index == index) {
								
								peer.removePeer();
								
								break;
								
							}
							
						}
						
					}
					
				}
				break;
				
			case "sendMoney":
				{

                    if (user.secretPhrase != null) {
						
						String recipientValue = req.getParameter("recipient"), amountValue = req.getParameter("amount"), feeValue = req.getParameter("fee"), deadlineValue = req.getParameter("deadline");
                        String secretPhrase = req.getParameter("secretPhrase");

                        long recipient;
						int amount = 0, fee = 0;
						short deadline = 0;
						
						try {
							
							recipient = (new BigInteger(recipientValue.trim())).longValue();
							amount = Integer.parseInt(amountValue.trim());
							fee = Integer.parseInt(feeValue.trim());
							deadline = (short)(Double.parseDouble(deadlineValue) * 60);
							
						} catch (Exception e) {
							
							JSONObject response = new JSONObject();
							response.put("response", "notifyOfIncorrectTransaction");
							response.put("message", "One of the fields is filled incorrectly!");
							response.put("recipient", recipientValue);
							response.put("amount", amountValue);
							response.put("fee", feeValue);
							response.put("deadline", deadlineValue);
							
							user.pendingResponses.offer(response);
							
							break;
							
						}

                        if (! user.secretPhrase.equals(secretPhrase)) {

                            JSONObject response = new JSONObject();
                            response.put("response", "notifyOfIncorrectTransaction");
                            response.put("message", "Wrong secret phrase!");
                            response.put("recipient", recipientValue);
                            response.put("amount", amountValue);
                            response.put("fee", feeValue);
                            response.put("deadline", deadlineValue);

                            user.pendingResponses.offer(response);

                        } else if (amount <= 0) {
							
							JSONObject response = new JSONObject();
							response.put("response", "notifyOfIncorrectTransaction");
							response.put("message", "\"Amount\" must be greater than 0!");
							response.put("recipient", recipientValue);
							response.put("amount", amountValue);
							response.put("fee", feeValue);
							response.put("deadline", deadlineValue);
							
							user.pendingResponses.offer(response);
							
						} else if (fee <= 0) {
							
							JSONObject response = new JSONObject();
							response.put("response", "notifyOfIncorrectTransaction");
							response.put("message", "\"Fee\" must be greater than 0!");
							response.put("recipient", recipientValue);
							response.put("amount", amountValue);
							response.put("fee", feeValue);
							response.put("deadline", deadlineValue);
							
							user.pendingResponses.offer(response);
							
						} else if (deadline < 1) {
							
							JSONObject response = new JSONObject();
							response.put("response", "notifyOfIncorrectTransaction");
							response.put("message", "\"Deadline\" must be greater or equal to 1 minute!");
							response.put("recipient", recipientValue);
							response.put("amount", amountValue);
							response.put("fee", feeValue);
							response.put("deadline", deadlineValue);
							
							user.pendingResponses.offer(response);
							
						} else {
							
							byte[] publicKey = Crypto.getPublicKey(user.secretPhrase);
							Account account = accounts.get(Account.getId(publicKey));
							if (account == null || (amount + fee) * 100L > account.unconfirmedBalance) {
								
								JSONObject response = new JSONObject();
								response.put("response", "notifyOfIncorrectTransaction");
								response.put("message", "Not enough funds!");
								response.put("recipient", recipientValue);
								response.put("amount", amountValue);
								response.put("fee", feeValue);
								response.put("deadline", deadlineValue);
								
								user.pendingResponses.offer(response);
								
							} else {
								
								final Transaction transaction = new Transaction(Transaction.TYPE_PAYMENT, Transaction.SUBTYPE_PAYMENT_ORDINARY_PAYMENT, getEpochTime(System.currentTimeMillis()), deadline, publicKey, recipient, amount, fee, 0, new byte[64]);
								transaction.sign(user.secretPhrase);
								
								JSONObject peerRequest = new JSONObject();
								peerRequest.put("requestType", "processTransactions");
								JSONArray transactionsData = new JSONArray();
								transactionsData.add(transaction.getJSONObject());
								peerRequest.put("transactions", transactionsData);
								
								Peer.sendToAllPeers(peerRequest);
								
								JSONObject response = new JSONObject();
								response.put("response", "notifyOfAcceptedTransaction");
								
								user.pendingResponses.offer(response);
								
							}
							
						}
						
					}
					
				}
				break;
				
			case "unlockAccount":
				{
					
					String secretPhrase = req.getParameter("secretPhrase");
					BigInteger accountId = user.initializeKeyPair(secretPhrase);
					
					JSONObject response = new JSONObject();
					response.put("response", "unlockAccount");
					response.put("account", accountId.toString());
					
					if (secretPhrase.length() < 30) {
						
						response.put("secretPhraseStrength", 1);
						
					} else {
						
						response.put("secretPhraseStrength", 5);
						
					}
					
					Account account = accounts.get(accountId.longValue());
					if (account == null) {
						
						response.put("balance", 0);
						
					} else {
						
						response.put("balance", account.unconfirmedBalance);
						
						if (account.getEffectiveBalance() > 0) {
							
							JSONObject response2 = new JSONObject();
							response2.put("response", "setBlockGenerationDeadline");
							
							Block lastBlock = Block.getLastBlock();
							MessageDigest digest = MessageDigest.getInstance("SHA-256");
							byte[] generationSignatureHash;
							if (lastBlock.height < TRANSPARENT_FORGING_BLOCK) {
								
								byte[] generationSignature = Crypto.sign(lastBlock.generationSignature, user.secretPhrase);
								generationSignatureHash = digest.digest(generationSignature);
								
							} else {
								
								digest.update(lastBlock.generationSignature);
								generationSignatureHash = digest.digest(Crypto.getPublicKey(user.secretPhrase));
								
							}
							BigInteger hit = new BigInteger(1, new byte[] {generationSignatureHash[7], generationSignatureHash[6], generationSignatureHash[5], generationSignatureHash[4], generationSignatureHash[3], generationSignatureHash[2], generationSignatureHash[1], generationSignatureHash[0]});
							response2.put("deadline", hit.divide(BigInteger.valueOf(Block.getBaseTarget()).multiply(BigInteger.valueOf(account.getEffectiveBalance()))).longValue() - (getEpochTime(System.currentTimeMillis()) - lastBlock.timestamp));
							
							user.pendingResponses.offer(response2);
							
						}
						
						JSONArray myTransactions = new JSONArray();

                        for (Transaction transaction : unconfirmedTransactions.values()) {

                            if (Account.getId(transaction.senderPublicKey) == accountId.longValue()) {

                                JSONObject myTransaction = new JSONObject();
                                myTransaction.put("index", transaction.index);
                                myTransaction.put("transactionTimestamp", transaction.timestamp);
                                myTransaction.put("deadline", transaction.deadline);
                                myTransaction.put("account", convert(transaction.recipient));
                                myTransaction.put("sentAmount", transaction.amount);
                                if (transaction.recipient == accountId.longValue()) {

                                    myTransaction.put("receivedAmount", transaction.amount);

                                }
                                myTransaction.put("fee", transaction.fee);
                                myTransaction.put("numberOfConfirmations", 0);
                                myTransaction.put("id", convert(transaction.getId()));

                                myTransactions.add(myTransaction);

                            } else if (transaction.recipient == accountId.longValue()) {

                                JSONObject myTransaction = new JSONObject();
                                myTransaction.put("index", transaction.index);
                                myTransaction.put("transactionTimestamp", transaction.timestamp);
                                myTransaction.put("deadline", transaction.deadline);
                                myTransaction.put("account", convert(Account.getId(transaction.senderPublicKey)));
                                myTransaction.put("receivedAmount", transaction.amount);
                                myTransaction.put("fee", transaction.fee);
                                myTransaction.put("numberOfConfirmations", 0);
                                myTransaction.put("id", convert(transaction.getId()));

                                myTransactions.add(myTransaction);

                            }

                        }

						long blockId = lastBlock;
						int numberOfConfirmations = 1;
						while (myTransactions.size() < 1000) {
							
							Block block = blocks.get(blockId);
							
							if (Account.getId(block.generatorPublicKey) == accountId.longValue() && block.totalFee > 0) {
								
								JSONObject myTransaction = new JSONObject();
								myTransaction.put("index", convert(blockId)); //???
								myTransaction.put("blockTimestamp", block.timestamp);
								myTransaction.put("block", convert(blockId));
								myTransaction.put("earnedAmount", block.totalFee);
								myTransaction.put("numberOfConfirmations", numberOfConfirmations);
								myTransaction.put("id", "-");
								
								myTransactions.add(myTransaction);
								
							}
							
							for (int i = 0; i < block.transactions.length; i++) {
								
								Transaction transaction = transactions.get(block.transactions[i]);
								if (Account.getId(transaction.senderPublicKey) == accountId.longValue()) {
									
									JSONObject myTransaction = new JSONObject();
									myTransaction.put("index", transaction.index);
									myTransaction.put("blockTimestamp", block.timestamp);
									myTransaction.put("transactionTimestamp", transaction.timestamp);
									myTransaction.put("account", convert(transaction.recipient));
									myTransaction.put("sentAmount", transaction.amount);
									if (transaction.recipient == accountId.longValue()) {
										
										myTransaction.put("receivedAmount", transaction.amount);
										
									}
									myTransaction.put("fee", transaction.fee);
									myTransaction.put("numberOfConfirmations", numberOfConfirmations);
									myTransaction.put("id", convert(transaction.getId()));
									
									myTransactions.add(myTransaction);
									
								} else if (transaction.recipient == accountId.longValue()) {
									
									JSONObject myTransaction = new JSONObject();
									myTransaction.put("index", transaction.index);
									myTransaction.put("blockTimestamp", block.timestamp);
									myTransaction.put("transactionTimestamp", transaction.timestamp);
									myTransaction.put("account", convert(Account.getId(transaction.senderPublicKey)));
									myTransaction.put("receivedAmount", transaction.amount);
									myTransaction.put("fee", transaction.fee);
									myTransaction.put("numberOfConfirmations", numberOfConfirmations);
									myTransaction.put("id", convert(transaction.getId()));
									
									myTransactions.add(myTransaction);
									
								}
								
							}
							
							if (blockId == GENESIS_BLOCK_ID) {
								
								break;
								
							}
							
							blockId = block.previousBlock;
							numberOfConfirmations++;
							
						}
						
						if (myTransactions.size() > 0) {
							
							JSONObject response2 = new JSONObject();
							response2.put("response", "processNewData");
							response2.put("addedMyTransactions", myTransactions);
							
							user.pendingResponses.offer(response2);
							
						}
						
					}
					
					user.pendingResponses.offer(response);
					
				}
				break;
				
			default:
				{
					
					JSONObject response = new JSONObject();
					response.put("response", "showMessage");
					response.put("message", "Incorrect request!");
					
					user.pendingResponses.offer(response);
					
				}
				
			}
			
		} catch (Exception e) {
			
			if (user != null) {
				
				JSONObject response = new JSONObject();
				response.put("response", "showMessage");
				response.put("message", e.toString());
				
				user.pendingResponses.offer(response);
				
			}
			
		}
		
		if (user != null) {
			
			synchronized (user) {
				
				JSONArray responses = new JSONArray();
				JSONObject pendingResponse;
				while ((pendingResponse = user.pendingResponses.poll()) != null) {
					
					responses.add(pendingResponse);
					
				}
				
				if (responses.size() > 0) {
					
					JSONObject combinedResponse = new JSONObject();
					combinedResponse.put("responses", responses);
					
					if (user.asyncContext != null) {
						
						user.asyncContext.getResponse().setContentType("text/plain; charset=UTF-8");

                        try (ServletOutputStream servletOutputStream = user.asyncContext.getResponse().getOutputStream()) {
                            servletOutputStream.write(combinedResponse.toString().getBytes("UTF-8"));
                        }
						user.asyncContext.complete();
						user.asyncContext = req.startAsync();
						user.asyncContext.addListener(new UserAsyncListener(user));
						user.asyncContext.setTimeout(5000);
						
					} else {
						
						resp.setContentType("text/plain; charset=UTF-8");

                        try (ServletOutputStream servletOutputStream = resp.getOutputStream()) {
                            servletOutputStream.write(combinedResponse.toString().getBytes("UTF-8"));
                        }
						
					}
					
				} else {
					
					if (user.asyncContext != null) {
						
						user.asyncContext.getResponse().setContentType("text/plain; charset=UTF-8");

                        try (ServletOutputStream servletOutputStream = user.asyncContext.getResponse().getOutputStream()) {
                            servletOutputStream.write((new JSONObject()).toString().getBytes("UTF-8"));
                        }
						user.asyncContext.complete();
						
					}
					
					user.asyncContext = req.startAsync();
					user.asyncContext.addListener(new UserAsyncListener(user));
					user.asyncContext.setTimeout(5000);
					
				}
				
			}
			
		}
		
	}
	
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		Peer peer = null;
		
		JSONObject response = new JSONObject();
		
		try {
			
			JSONObject request;
			{

                //TODO: change to have the JSON parser read from the input stream via a reader directly,
                //instead of creating an intermediate byte[] and String
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                byte[] buffer = new byte[65536];
                int numberOfBytes;

				try (InputStream inputStream = req.getInputStream()) {

                    while ((numberOfBytes = inputStream.read(buffer)) > 0) {

                        byteArrayOutputStream.write(buffer, 0, numberOfBytes);

                    }
                }
				request = (JSONObject)JSONValue.parse(byteArrayOutputStream.toString("UTF-8"));
				
				peer = Peer.addPeer(req.getRemoteHost(), "");
				if (peer != null) {
					
					if (peer.state == Peer.STATE_DISCONNECTED) {
						
						peer.setState(Peer.STATE_CONNECTED);
						
					}
					peer.updateDownloadedVolume(byteArrayOutputStream.size());
					
				}
				
			}
			
			if (((Long)request.get("protocol")) == 1) {
				
				switch ((String)request.get("requestType")) {
				
				case "getCumulativeDifficulty":
					{
						
						response.put("cumulativeDifficulty", Block.getLastBlock().cumulativeDifficulty.toString());
						
					}
					break;
					
				case "getInfo":
					{
						
						String announcedAddress = (String)request.get("announcedAddress");
						if (announcedAddress != null) {
							
							announcedAddress = announcedAddress.trim();
							if (announcedAddress.length() > 0) {
								
								peer.announcedAddress = announcedAddress;
								
							}
							
						}
						if (peer != null) {
							
							String application = (String)request.get("application");
							if (application == null) {
								
								application = "?";
								
							} else {
								
								application = application.trim();
								if (application.length() > 20) {
									
									application = application.substring(0, 20) + "...";
									
								}
								
							}
							peer.application = application;
							
							String version = (String)request.get("version");
							if (version == null) {
								
								version = "?";
								
							} else {
								
								version = version.trim();
								if (version.length() > 10) {
									
									version = version.substring(0, 10) + "...";
									
								}
								
							}
							peer.version = version;
							
							String platform = (String)request.get("platform");
							if (platform == null) {
								
								platform = "?";
								
							} else {
								
								platform = platform.trim();
								if (platform.length() > 10) {
									
									platform = platform.substring(0, 10) + "...";
									
								}
								
							}
							peer.platform = platform;
							
							try {
								
								peer.shareAddress = Boolean.parseBoolean((String)request.get("shareAddress"));
								
							} catch (Exception e) { }
							
							if (peer.analyzeHallmark(req.getRemoteHost(), (String)request.get("hallmark"))) {
								
								peer.setState(Peer.STATE_CONNECTED);
								
							} else {
								
								peer.blacklist();
								
							}
							
						}
						
						if (myHallmark != null && myHallmark.length() > 0) {
							
							response.put("hallmark", myHallmark);
							
						}
						response.put("application", "NRS");
						response.put("version", VERSION);
						response.put("platform", myPlatform);
						response.put("shareAddress", shareMyAddress);
						
					}
					break;
					
				case "getMilestoneBlockIds":
					{
						
						JSONArray milestoneBlockIds = new JSONArray();
						Block block = Block.getLastBlock();
						int jumpLength = block.height * 4 / 1461 + 1;
						while (block.height > 0) {
							
							milestoneBlockIds.add(convert(block.getId()));
							for (int i = 0; i < jumpLength && block.height > 0; i++) {
								
								block = blocks.get(block.previousBlock);
								
							}
							
						}
						response.put("milestoneBlockIds", milestoneBlockIds);
						
					}
					break;
					
				case "getNextBlockIds":
					{
						
						JSONArray nextBlockIds = new JSONArray();
						Block block = blocks.get((new BigInteger((String)request.get("blockId")).longValue()));
						while (block != null && nextBlockIds.size() < 1440) {
							
							block = blocks.get(block.nextBlock);
							if (block != null) {
								
								nextBlockIds.add(convert(block.getId()));
								
							}
							
						}
						response.put("nextBlockIds", nextBlockIds);
						
					}
					break;
					
				case "getNextBlocks":
					{
						
						LinkedList<Block> nextBlockIds = new LinkedList<>();
						int totalLength = 0;
						Block block = blocks.get((new BigInteger((String)request.get("blockId")).longValue()));
						while (block != null) {
							
							block = blocks.get(block.nextBlock);
							if (block != null) {
								
								int length = BLOCK_HEADER_LENGTH + block.payloadLength;
								if (totalLength + length > 1048576) {
									
									break;
									
								}
								
								nextBlockIds.add(block);
								totalLength += length;
								
							}
							
						}
						
						JSONArray nextBlocks = new JSONArray();
						for (int i = 0; i < nextBlockIds.size(); i++) {
							
							nextBlocks.add(nextBlockIds.get(i).getJSONObject(transactions));
							
						}
						response.put("nextBlocks", nextBlocks);
						
					}
					break;
					
				case "getPeers":
					{
						
						JSONArray peers = new JSONArray();
						for (Peer otherPeer : Nxt.peers.values()) {
							
							if (otherPeer.blacklistingTime == 0 && otherPeer.announcedAddress.length() > 0 && otherPeer.state == Peer.STATE_CONNECTED && otherPeer.shareAddress) {
								
								peers.add(otherPeer.announcedAddress);
								
							}
							
						}
						response.put("peers", peers);
						
					}
					break;
					
				case "getUnconfirmedTransactions":
					{
						
						JSONArray transactionsData = new JSONArray();
						for (Transaction transaction : unconfirmedTransactions.values()) {
							
							transactionsData.add(transaction.getJSONObject());
							
						}
						response.put("unconfirmedTransactions", transactionsData);
						
					}
					break;
					
				case "processBlock":
					{
						
						int version = ((Long)request.get("version")).intValue();
						int blockTimestamp = ((Long)request.get("timestamp")).intValue();
						long previousBlock = (new BigInteger((String)request.get("previousBlock"))).longValue();
						int numberOfTransactions = ((Long)request.get("numberOfTransactions")).intValue();
						int totalAmount = ((Long)request.get("totalAmount")).intValue();
						int totalFee = ((Long)request.get("totalFee")).intValue();
						int payloadLength = ((Long)request.get("payloadLength")).intValue();
						byte[] payloadHash = convert((String)request.get("payloadHash"));
						byte[] generatorPublicKey = convert((String)request.get("generatorPublicKey"));
						byte[] generationSignature = convert((String)request.get("generationSignature"));
						byte[] blockSignature = convert((String)request.get("blockSignature"));
						
						Block block;
						if (version == 1) {
							
							block = new Block(version, blockTimestamp, previousBlock, numberOfTransactions, totalAmount, totalFee, payloadLength, payloadHash, generatorPublicKey, generationSignature, blockSignature);
							
						} else {
							
							byte[] previousBlockHash = convert((String)request.get("previousBlockHash"));
							
							block = new Block(version, blockTimestamp, previousBlock, numberOfTransactions, totalAmount, totalFee, payloadLength, payloadHash, generatorPublicKey, generationSignature, blockSignature, previousBlockHash);
							
						}
						
						ByteBuffer buffer = ByteBuffer.allocate(BLOCK_HEADER_LENGTH + payloadLength);
						buffer.order(ByteOrder.LITTLE_ENDIAN);
						
						buffer.put(block.getBytes());
						
						JSONArray transactionsData = (JSONArray)request.get("transactions");
						for (int i = 0; i < transactionsData.size(); i++) {
							
							buffer.put(Transaction.getTransaction((JSONObject)transactionsData.get(i)).getBytes());
							
						}
						
						boolean accepted = Block.pushBlock(buffer, true);
						response.put("accepted", accepted);
						
					}
					break;
					
				case "processTransactions":
					{
						
						Transaction.processTransactions(request, "transactions");
						
					}
					break;
					
				default:
					{
						
						response.put("error", "Unsupported request type!");
						
					}
					
				}
				
			} else {
				
				response.put("error", "Unsupported protocol!");
				
			}
			
		} catch (Exception e) {
			
			response.put("error", e.toString());
			
		}
		
		resp.setContentType("text/plain; charset=UTF-8");
		
		byte[] responseBytes = response.toString().getBytes("UTF-8");
		try (ServletOutputStream servletOutputStream = resp.getOutputStream()) {
    		servletOutputStream.write(responseBytes);
        }

        /* //TODO: I would rewrite the above to avoid the creation of byte[] and to avoid JSONObject.toString()
        DataOutputStream os = new DataOutputStream(resp.getOutputStream());
        Writer writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
        try {
            response.writeJSONString(writer);
        } finally {
            writer.close();
        }
        */

		if (peer != null) {
			
			peer.updateUploadedVolume(responseBytes.length);
			//peer.updateUploadedVolume(os.size());
		}
		
	}
	
	@Override
	public void destroy() {
		
		scheduledThreadPool.shutdown();
        try {
            scheduledThreadPool.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (! scheduledThreadPool.isTerminated()) {
            logMessage("some threads didn't terminate, forcing shutdown");
            scheduledThreadPool.shutdownNow();
        }

		try {
			
			Block.saveBlocks("blocks.nxt", true);
			
		} catch (Exception e) {
            logMessage("error saving blocks.nxt");
        }

		try {
			
			Transaction.saveTransactions("transactions.nxt");
			
		} catch (Exception e) {
            logMessage("error saving transactions.nxt");
        }

        /* no longer used
		try {
			
			blockchainChannel.close();
			
		} catch (Exception e) { }
		*/

		logMessage("Nxt stopped.");
		
	}
	
}
