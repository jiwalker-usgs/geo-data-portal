Ext.ns("GDP");

GDP.MapActivityBar = Ext.extend(Ext.Toolbar, {
    layerController : undefined,
    constructor : function(config) {
        LOG.debug('MapActivityBar:constructor: Constructing self.');
        
        if (!config) config = {};
        
        this.layerController = config.layerController;
        
        var map = config.map;
        var toggleGroup = 'draw';
        var zoomToExtentAction, navigationAction, bboxVector, drawBboxAction;
        
        bboxVector = new OpenLayers.Layer.Vector('bboxvector');
        map.addLayers([bboxVector]);
        
        var control = new OpenLayers.Control();
        OpenLayers.Util.extend(control, {
            draw: function() {
                this.handler = new OpenLayers.Handler.Box(
                    control,
                    {
                        done : this.notice
                    },
                    {
                        alwaysZoom : true
                    }
                
                    )
                this.handler.activate();
            },
            notice : function(xy) {
                LOG.debug(xy);
            }
        });
        
        
        drawBboxAction = new GeoExt.Action({
            text: 'Draw Box'
            ,control: control
            ,toggleGroup: toggleGroup
            ,allowDepress: false
            ,tooltip: 'Draw A Bounding Box On The Map'
            ,group: toggleGroup
            ,map: map
        });
        
//        map.addControl(new OpenLayers.Control.EditingToolbar(bboxVector));
        zoomToExtentAction = new GeoExt.Action({
            text : 'Max Extent'
            ,control: new OpenLayers.Control.ZoomToMaxExtent()
            ,tooltip: 'Zoom To Map Extent'
            ,map: map
        });
        
        navigationAction = new GeoExt.Action({
            text: 'Nav'
            ,control: new OpenLayers.Control.Navigation()
            ,toggleGroup: toggleGroup
            ,allowDepress: false
            ,pressed: true
            ,tooltip: 'Navigate The Map'
            ,group: toggleGroup
            ,checked: true
            ,map: map
        });
        
        var zoomAction = new GeoExt.Action({
            text: "Zoom In"
            ,control: new OpenLayers.Control.ZoomBox({alwaysZoom: true})
            ,map: map
            ,toggleGroup: toggleGroup
            ,allowDepress: false
            ,tooltip: "Zoom In"
            ,group: toggleGroup
        });
        
        config = Ext.apply({
            items: [zoomToExtentAction, navigationAction, drawBboxAction, zoomAction]
        }, config);
        GDP.MapActivityBar.superclass.constructor.call(this, config);
        LOG.debug('MapActivityBar:constructor: Construction complete.');
    }
});