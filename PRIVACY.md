# TVora privacy

TVora has no account, analytics, advertising or developer-operated server. TV data and recovery
records stay in the app's private phone storage unless the owner exports or shares them.

The app discovers and connects to the selected TV on the local network using legacy ADB or
experimental TLS wireless debugging. TV authorization remains under the owner's control.
Pairing codes are used for the current exchange and are not saved. The TLS identity is in Android
Keystore. Reset my key clears both legacy and wireless identities.

A photo selected through the system picker is copied privately, bounded in size, decoded with
camera orientation handled, re-encoded without original metadata, and sent only to the selected TV.
Delete everything removes phone photo copies, local TV records and pairing identities. Deleting
phone data is not the same as restoring the TV; use the restore flow first if required.

The full build contacts GitHub only for explicit add-on actions from the inherited signed index.
TVora does not check the upstream app-update feed. The Play build omits add-on downloads. A copied
or shared report leaves the phone only through the owner's chosen action; review the preview first.

Permissions support local networking/discovery, notifications, an active-operation foreground
service and the device credential prompt. Photo import uses a single-item system picker, not
gallery-wide access. The app requests no contacts, camera or microphone permission.
