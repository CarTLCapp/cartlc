/**
 * Copyright 2018, FleetTLC. All rights reserved
 */
package com.cartlc.tracker.fresh.model.event

/**
 * Created by dug on 5/31/17.
 */

class EventError(private val mMessage: String) : EventCommon() {

    override fun toString(): String {
        return mMessage
    }
}
