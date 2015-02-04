/**
 * @depends {nrs.js}
 */
var NRS = (function(NRS, $, undefined) {
    NRS.transactionTypes = {
        0: {
            'title': "Payment",
            'i18nKeyTitle': 'payment',
            'iconHTML': "<i class='ion-calculator'></i>",
            'subTypes': {
                0: {
                    'title': "Ordinary Payment",
                    'i18nKeyTitle': 'ordinary_payment',
                    'iconHTML': "<i class='fa fa-money'></i>"
                }
            }
        },
        1: {
            'title': "Messaging/Voting/Aliases",
            'i18nKeyTitle': 'messaging_voting_aliases',
            'iconHTML': "<i class='fa fa-envelope-square'></i>",
            'subTypes': {
                0: {
                    'title': "Arbitrary Message",
                    'i18nKeyTitle': 'arbitrary_message',
                    'iconHTML': "<i class='fa fa-envelope-o'></i>"
                },
                1: {
                    'title': "Alias Assignment",
                    'i18nKeyTitle': 'alias_assignment',
                    'iconHTML': "<i class='fa fa-bookmark'></i>"
                },
                2: {
                    'title': "Poll Creation",
                    'i18nKeyTitle': 'poll_creation',
                    'iconHTML': "<i class='fa fa-check-square-o'></i>"
                },
                3: {
                    'title': "Vote Casting",
                    'i18nKeyTitle': 'vote_casting',
                    'iconHTML': "<i class='fa fa-check'></i>"
                },
                4: {
                    'title': "Hub Announcement",
                    'i18nKeyTitle': 'hub_announcement',
                    'iconHTML': "<i class='ion-radio-waves'></i>"
                },
                5: {
                    'title': "Account Info",
                    'i18nKeyTitle': 'account_info',
                    'iconHTML': "<i class='fa fa-info'></i>"
                },
                6: {
                    'title': "Alias Sale",
                    'i18nKeyTitle': 'alias_sale',
                    'iconHTML': "<i class='fa fa-tag'></i>"
                },
                7: {
                    'title': "Alias Buy",
                    'i18nKeyTitle': 'alias_buy',
                    'iconHTML': "<i class='fa fa-money'></i>"
                },
                8: {
                    'title': "Alias Deletion",
                    'i18nKeyTitle': 'alias_deletion',
                    'iconHTML': "<i class='fa fa-times'></i>"
                }
            }
        },
        2: {
            'title': "Asset Exchange",
            'i18nKeyTitle': 'asset_exchange',
            'iconHTML': '<i class="fa fa-signal"></i>',
            'subTypes': {
                0: {
                    'title': "Asset Issuance",
                    'i18nKeyTitle': 'asset_issuance',
                    'iconHTML': '<i class="fa fa-bullhorn"></i>'
                },
                1: {
                    'title': "Asset Transfer",
                    'i18nKeyTitle': 'asset_transfer',
                    'iconHTML': '<i class="ion-arrow-swap"></i>'
                },
                2: {
                    'title': "Ask Order Placement",
                    'i18nKeyTitle': 'ask_order_placement',
                    'iconHTML': '<i class="ion-arrow-graph-down-right"></i>'
                },
                3: {
                    'title': "Bid Order Placement",
                    'i18nKeyTitle': 'bid_order_placement',
                    'iconHTML': '<i class="ion-arrow-graph-up-right"></i>'
                },
                4: {
                    'title': "Ask Order Cancellation",
                    'i18nKeyTitle': 'ask_order_cancellation',
                    'iconHTML': '<i class="fa fa-times"></i>'
                },
                5: {
                    'title': "Bid Order Cancellation",
                    'i18nKeyTitle': 'bid_order_cancellation',
                    'iconHTML': '<i class="fa fa-times"></i>'
                },
                6: {
                    'title': "Dividend Payment",
                    'i18nKeyTitle': 'dividend_payment',
                    'iconHTML': '<i class="fa fa-gift"></i>'
                }
            }
        },
        3: {
            'title': "Marketplace",
            'i18nKeyTitle': 'marketplace',
            'iconHTML': '<i class="fa fa-shopping-cart"></i>',
            'subTypes': {
                0: {
                    'title': "Marketplace Listing",
                    'i18nKeyTitle': 'marketplace_listing',
                    'iconHTML': '<i class="fa fa-bullhorn"></i>'
                },
                1: {
                    'title': "Marketplace Removal",
                    'i18nKeyTitle': 'marketplace_removal',
                    'iconHTML': '<i class="fa fa-times"></i>'
                },
                2: {
                    'title': "Marketplace Price Change",
                    'i18nKeyTitle': 'marketplace_price_change',
                    'iconHTML': '<i class="fa fa-line-chart"></i>'
                },
                3: {
                    'title': "Marketplace Quantity Change",
                    'i18nKeyTitle': 'marketplace_quantity_change',
                    'iconHTML': '<i class="fa fa-sort"></i>'
                },
                4: {
                    'title': "Marketplace Purchase",
                    'i18nKeyTitle': 'marketplace_purchase',
                    'iconHTML': '<i class="fa fa-money"></i>'
                },
                5: {
                    'title': "Marketplace Delivery",
                    'i18nKeyTitle': 'marketplace_delivery',
                    'iconHTML': '<i class="fa fa-cube"></i>'
                },
                6: {
                    'title': "Marketplace Feedback",
                    'i18nKeyTitle': 'marketplace_feedback',
                    'iconHTML': '<i class="ion-android-social"></i>'
                },
                7: {
                    'title': "Marketplace Refund",
                    'i18nKeyTitle': 'marketplace_refund',
                    'iconHTML': '<i class="fa fa-reply"></i>'
                }
            }
        },
        4: {
            'title': "Account Control",
            'i18nKeyTitle': 'account_control',
            'iconHTML': '<i class="ion-locked"></i>',
            'subTypes': {
                0: {
                    'title': "Balance Leasing",
                    'i18nKeyTitle': 'balance_leasing',
                    'iconHTML': '<i class="fa fa-arrow-circle-o-right"></i>'
                }
            }
        },
        5: {
            'title': "Monetary System",
            'i18nKeyTitle': 'monetary_system',
            'iconHTML': '<i class="fa fa-bank"></i>',
            'subTypes': {
                0: {
                    'title': "Issue Currency",
                    'i18nKeyTitle': 'issue_currency',
                    'iconHTML': '<i class="fa fa-bullhorn"></i>'
                },
                1: {
                    'title': "Reserve Increase",
                    'i18nKeyTitle': 'reserve_increase',
                    'iconHTML': '<i class="fa fa-cubes"></i>'
                },
                2: {
                    'title': "Reserve Claim",
                    'i18nKeyTitle': 'reserve_claim',
                    'iconHTML': '<i class="fa fa-truck"></i>'
                },
                3: {
                    'title': "Currency Transfer",
                    'i18nKeyTitle': 'currency_transfer',
                    'iconHTML': '<i class="ion-arrow-swap"></i>'
                },
                4: {
                    'title': "Publish Exchange Offer",
                    'i18nKeyTitle': 'publish_exchange_offer',
                    'iconHTML': '<i class="fa fa-list-alt "></i>'
                },
                5: {
                    'title': "Buy Currency",
                    'i18nKeyTitle': 'currency_buy',
                    'iconHTML': '<i class="ion-arrow-graph-up-right"></i>'
                },
                6: {
                    'title': "Sell Currency",
                    'i18nKeyTitle': 'currency_sell',
                    'iconHTML': '<i class="ion-arrow-graph-down-right"></i>'
                },
                7: {
                    'title': "Mint Currency",
                    'i18nKeyTitle': 'mint_currency',
                    'iconHTML': '<i class="fa fa-money"></i>'
                },
                8: {
                    'title': "Delete Currency",
                    'i18nKeyTitle': 'delete_currency',
                    'iconHTML': '<i class="fa fa-times"></i>'
                }
            }
        },
    }


return NRS;
}(NRS || {}, jQuery));