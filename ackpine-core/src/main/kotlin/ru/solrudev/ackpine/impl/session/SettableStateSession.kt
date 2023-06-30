package ru.solrudev.ackpine.impl.session

import ru.solrudev.ackpine.session.Failure
import ru.solrudev.ackpine.session.Session

internal interface SettableStateSession<F : Failure> : Session<F> {
	var state: Session.State<F>
}