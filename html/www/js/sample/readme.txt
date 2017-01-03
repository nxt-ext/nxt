Sample NXT APIs for Node JS
===========================

The sample code performs local signing and encryption using
the same code used by the official NXT wallet.
Your passphrase is never sent to the remote node when submitting
a transaction.

It is only sent to the remote node when invoking
/startForging or other specific APIs which require sending
the passphrase to the remote node. Don't use such APIs.

To use the samples:
1. No need to install the NXT software or download the blockchain
2. Install Node JS and NPM from https://nodejs.org/en/
3. From the command prompt type:
npm install nxt-blockchain
cd node_modules/nxt-blockchain/sample

4. Configure the remote node and NXT account by editing the config.json file
5. Run the samples using Node JS, for example:
node send.money.js