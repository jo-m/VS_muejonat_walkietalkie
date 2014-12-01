Clients can have states:

* `disconnected`
* `group controller`
* `group member`

Global state can be:

* `group formed`
* `no group formed`

# State determination Algo
How to determine the current state

    Loop through all buddies
    Look if one is group controller
    If yes:
    	set him as group controller
    If Not:
    	we are group controller? Are we even connected?

# Connection Algo
A basic routine to run when we search a new connection

	-> determine global state
	if no group formed: form new group
	if group formed: wait for invitation

# Group Controller Algo
A routine which the group controller runs periodically

	look if there are disconnected peers
	-> invite them into our group
	look if there are other groups using the service
	-> maybe close our group, and add our members to the other group?

# Group Member Algo
A routine which group members run periodically

	same as Group Controller Algo

# New connection
Set new state

# Lost a connection
	if group controller:
		do nothing, the missing client will be removed from the list
		on next peer-list-changed notification
	else:
		state = disconnected

# Peer-list-update
--> Remove all the peers from buddies list which are not listed anymore in this list  
Does not matter if we are group controller or common client
