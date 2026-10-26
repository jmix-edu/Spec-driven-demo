package com.company.booking.view.room;

import com.company.booking.entity.Room;
import com.company.booking.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.*;


@ViewController(id = "Room.lookup")
@ViewDescriptor(path = "room-lookup-view.xml")
@LookupComponent("roomsDataGrid")
@DialogMode(width = "64em")
public class RoomLookupView extends StandardListView<Room> {

}