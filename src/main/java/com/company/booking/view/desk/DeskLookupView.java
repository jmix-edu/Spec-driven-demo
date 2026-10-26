package com.company.booking.view.desk;

import com.company.booking.entity.Desk;
import com.company.booking.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.*;


@ViewController(id = "Desk.lookup")
@ViewDescriptor(path = "desk-lookup-view.xml")
@LookupComponent("desksDataGrid")
@DialogMode(width = "64em")
public class DeskLookupView extends StandardListView<Desk> {

}