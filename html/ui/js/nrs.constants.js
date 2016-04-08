/******************************************************************************
 * Copyright © 2013-2016 The Nxt Core Developers.                             *
 *                                                                            *
 * See the AUTHORS.txt, DEVELOPER-AGREEMENT.txt and LICENSE.txt files at      *
 * the top-level directory of this distribution for the individual copyright  *
 * holder information and the developer policies on copyright and licensing.  *
 *                                                                            *
 * Unless otherwise agreed in a custom licensing agreement, no part of the    *
 * Nxt software, including this file, may be copied, modified, propagated,    *
 * or distributed except according to the terms contained in the LICENSE.txt  *
 * file.                                                                      *
 *                                                                            *
 * Removal or modification of this copyright notice is prohibited.            *
 *                                                                            *
 ******************************************************************************/

/**
 * @depends {nrs.js}
 */
var NRS = (function (NRS, $) {
    NRS.constants = {
        'DB_VERSION': 2,

        'PLUGIN_VERSION': 1,
        'MAX_SHORT_JAVA': 32767,
        'MAX_UNSIGNED_SHORT_JAVA': 65535,
        'MAX_INT_JAVA': 2147483647,
        'MIN_PRUNABLE_MESSAGE_LENGTH': 28,
        'DISABLED_API_ERROR_CODE': 16,

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

        'VOTING_MODELS': {},
        'MIN_BALANCE_MODELS': {},
        "HASH_ALGORITHMS": {},
        "PHASING_HASH_ALGORITHMS": {},
        "MINTING_HASH_ALGORITHMS": {},
        "REQUEST_TYPES": {},
        "API_TAGS": {},

        'SERVER': {},
        'MAX_TAGGED_DATA_DATA_LENGTH': 0,
        'MAX_PRUNABLE_MESSAGE_LENGTH': 0,
        'GENESIS': '',
        'GENESIS_RS': '',
        'EPOCH_BEGINNING': 0,
        'FORGING': 'forging',
        'NOT_FORGING': 'not_forging',
        'UNKNOWN': 'unknown'
    };

    NRS.loadAlgorithmList = function (algorithmSelect, isPhasingHash) {
        var hashAlgorithms;
        if (isPhasingHash) {
            hashAlgorithms = NRS.constants.PHASING_HASH_ALGORITHMS;
        } else {
            hashAlgorithms = NRS.constants.HASH_ALGORITHMS;
        }
        for (var key in hashAlgorithms) {
            if (hashAlgorithms.hasOwnProperty(key)) {
                algorithmSelect.append($("<option />").val(hashAlgorithms[key]).text(key));
            }
        }
    };

    NRS.loadServerConstants = function () {
        NRS.sendRequest("getConstants", {}, function (response) {
            if (response.genesisAccountId) {
                NRS.constants.SERVER = response;
                NRS.constants.VOTING_MODELS = response.votingModels;
                NRS.constants.MIN_BALANCE_MODELS = response.minBalanceModels;
                NRS.constants.HASH_ALGORITHMS = response.hashAlgorithms;
                NRS.constants.PHASING_HASH_ALGORITHMS = response.phasingHashAlgorithms;
                NRS.constants.MINTING_HASH_ALGORITHMS = response.mintingHashAlgorithms;
                NRS.constants.MAX_TAGGED_DATA_DATA_LENGTH = response.maxTaggedDataDataLength;
                NRS.constants.MAX_PRUNABLE_MESSAGE_LENGTH = response.maxPrunableMessageLength;
                NRS.constants.GENESIS = response.genesisAccountId;
                NRS.constants.GENESIS_RS = NRS.convertNumericToRSAccountFormat(response.genesisAccountId);
                NRS.constants.EPOCH_BEGINNING = response.epochBeginning;
                NRS.constants.REQUEST_TYPES = response.requestTypes;
                NRS.constants.API_TAGS = response.apiTags;
                NRS.constants.SHUFFLING_STAGES = response.shufflingStages;
                NRS.constants.SHUFFLING_PARTICIPANTS_STATES = response.shufflingParticipantStates;
                NRS.constants.DISABLED_APIS = response.disabledAPIs;
                NRS.constants.DISABLED_API_TAGS = response.disabledAPITags;
                NRS.loadTransactionTypeConstants(response);
            }
        }, false);
    };

    function getKeyByValue(map, value) {
        for (var key in map) {
            if (map.hasOwnProperty(key)) {
                if (value === map[key]) {
                    return key;
                }
            }
        }
        return null;
    }

    NRS.getVotingModelName = function (code) {
        return getKeyByValue(NRS.constants.VOTING_MODELS, code);
    };

    NRS.getVotingModelCode = function (name) {
        return NRS.constants.VOTING_MODELS[name];
    };

    NRS.getMinBalanceModelName = function (code) {
        return getKeyByValue(NRS.constants.MIN_BALANCE_MODELS, code);
    };

    NRS.getMinBalanceModelCode = function (name) {
        return NRS.constants.MIN_BALANCE_MODELS[name];
    };

    NRS.getHashAlgorithm = function (code) {
        return getKeyByValue(NRS.constants.HASH_ALGORITHMS, code);
    };

    NRS.getShufflingStage = function (code) {
        return getKeyByValue(NRS.constants.SHUFFLING_STAGES, code);
    };

    NRS.getShufflingParticipantState = function (code) {
        return getKeyByValue(NRS.constants.SHUFFLING_PARTICIPANTS_STATES, code);
    };

    NRS.isRequireBlockchain = function(requestType) {
        if (!NRS.constants.REQUEST_TYPES[requestType]) {
            // For requests invoked before the getConstants request returns,
            // we implicitly assume that they do not require the blockchain
            return false;
        }
        return true == NRS.constants.REQUEST_TYPES[requestType].requireBlockchain;
    };

    NRS.isRequirePost = function(requestType) {
        if (!NRS.constants.REQUEST_TYPES[requestType]) {
            // For requests invoked before the getConstants request returns
            // we implicitly assume that they can use GET
            return false;
        }
        return true == NRS.constants.REQUEST_TYPES[requestType].requirePost;
    };

    NRS.isRequestTypeEnabled = function(requestType) {
        if ($.isEmptyObject(NRS.constants.REQUEST_TYPES)) {
            return true;
        }
        if (requestType.indexOf("+") > 0) {
            requestType = requestType.substring(0, requestType.indexOf("+"));
        }
        return !!NRS.constants.REQUEST_TYPES[requestType];
    };

    NRS.isSubmitPassphrase = function (requestType) {
        return requestType == "startForging" ||
            requestType == "stopForging" ||
            requestType == "startShuffler" ||
            requestType == "getForging" ||
            requestType == "markHost";
    };

    NRS.isFileUploadRequest = function (requestType) {
        return requestType == "uploadTaggedData" || requestType == "dgsListing";
    };

    NRS.getFileUploadConfig = function (requestType) {
        var config = {};
        if (requestType == "uploadTaggedData") {
            config.selector = "#upload_file";
            config.requestParam = "file";
            config.errorDescription = "error_file_too_big";
            config.maxSize = NRS.constants.MAX_TAGGED_DATA_DATA_LENGTH;
        } else if (requestType == "dgsListing") {
            config.selector = "#dgs_listing_image";
            config.requestParam = "messageFile";
            config.errorDescription = "error_image_too_big";
            config.maxSize = NRS.constants.MAX_PRUNABLE_MESSAGE_LENGTH;
        }
        return config;
    };

    NRS.isApiEnabled = function(depends) {
        if (!depends) {
            return true;
        }
        var tags = depends.tags;
        if (tags) {
            for (var i=0; i < tags.length; i++) {
                if (!tags[i].enabled) {
                    return false;
                }
            }
        }
        var apis = depends.apis;
        if (apis) {
            for (i=0; i < apis.length; i++) {
                if (apis[i] && !apis[i].enabled) {
                    return false;
                }
            }
        }
        return true;
    };

    return NRS;
}(NRS || {}, jQuery));