/**
 * @depends {nrs.js}
 */
var NRS = (function (NRS, $, undefined) {
   NRS.constants = {
      'DB_VERSION': 2,

      'PLUGIN_VERSION': 1,
      'MAX_INT_JAVA': 2147483647,
      'MIN_PRUNABLE_MESSAGE_LENGTH': 28,

      //Plugin launch status numbers
      'PL_RUNNING': 1,
      'PL_PAUSED': 2,
      'PL_DEACTIVATED': 3,
      'PL_HALTED': 4,

      //Plugin validity status codes
      'PV_VALID': 100,
      'PV_NOT_VALID': 300,
      'PV_UNKNOWN_MANIFEST_VERSION': 301,
      'PV_INCOMPATIBLE_MANIFEST_VERSION': 302,
      'PV_INVALID_MANIFEST_FILE': 303,
      'PV_INVALID_MISSING_FILES': 304,
      'PV_INVALID_JAVASCRIPT_FILE': 305,

      //Plugin NRS compatibility status codes
      'PNC_COMPATIBLE': 100,
      'PNC_COMPATIBILITY_MINOR_RELEASE_DIFF': 101,
      'PNC_COMPATIBILITY_WARNING': 200,
      'PNC_COMPATIBILITY_MAJOR_RELEASE_DIFF': 202,
      'PNC_NOT_COMPATIBLE': 300,
      'PNC_COMPATIBILITY_UNKNOWN': 301,
      'PNC_COMPATIBILITY_CLIENT_VERSION_TOO_OLD': 302,

      'VOTING_MODEL': {
         NONE: {code: -1, name: "none"},
         ACCOUNT: {code: 0, name: "account"},
         NQT: {code: 1, name: "balance"},
         ASSET: {code: 2, name: "asset"},
         CURRENCY: {code: 3, name: "currency"},
         TRANSACTION: {code: 4, name: "transaction"},
         HASH: {code: 5, name: "hash"}
      },

      'MIN_BALANCE_MODEL': {
         NONE: {code: 0, name: "none"},
         NQT: {code: 1, name: "balance"},
         ASSET: {code: 2, name: "asset"},
         CURRENCY: {code: 3, name: "currency"}
      },

      'SERVER': {}
   };

   NRS.loadServerConstants = function () {
      NRS.sendRequest("getConstants", {}, function (response) {
         if (response.genesisAccountId) {
            NRS.constants.SERVER = response;
         }
      });
   };

   NRS.getVotingModel = function(code) {
      for (var model in NRS.constants.VOTING_MODEL) {
         if (code == NRS.constants.VOTING_MODEL[model].code) {
            return NRS.constants.VOTING_MODEL[model];
         }
      }
      return -1;
   };

   NRS.getMinBalanceModel = function(code) {
      for (var model in NRS.constants.MIN_BALANCE_MODEL) {
         if (code == NRS.constants.MIN_BALANCE_MODEL[model].code) {
            return NRS.constants.MIN_BALANCE_MODEL[model];
         }
      }
      return -1;
   };

   return NRS;
}(NRS || {}, jQuery));