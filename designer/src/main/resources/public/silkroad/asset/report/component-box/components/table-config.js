define(["report/component-box/components/table-vm-template"],function(a){function b(a){return{event:{rid:a,name:"submit"},action:{name:"sync"},argHandlers:[["clear"],["getValue","snpt.cnpt-form"]]}}var c=[{configType:"DI_TABLE",clzType:"COMPONENT",clzKey:"DI_TABLE",sync:{viewDisable:"ALL"},vuiRef:{},interactions:[{events:[{rid:"snpt.form",name:"dataloaded"},{rid:"snpt.form",name:"submit"}],action:{name:"sync"},argHandlers:[["clear"],["getValue","snpt.cnpt-form"]]}]},{clzType:"VUI",clzKey:"OLAP_TABLE",name:"table",dataOpt:{rowHCellCut:30,hCellCut:30,cCellCut:30,vScroll:!0,rowCheckMode:"SELECT"}},{clzType:"VUI",clzKey:"BREADCRUMB",dataOpt:{maxShow:6}},{clzType:"VUI",clzKey:"H_BUTTON",dataOpt:{skin:"ui-download-btn",text:"下载数据"}},{clzType:"VUI",clzKey:"TEXT_LABEL",dataInitOpt:{hide:!0}}],d=function(a){var d=a.rootId+a.serverData.id,e=$.extend(!0,[],c);return e[0].id=d,e[0].vuiRef={mainTable:d+"-vu-table",breadcrumb:d+"-vu-table-breadcrumb"},a.hasTableMeta&&e[0].interactions.push(b(a.rootId+a.serverData.selectionAreaId+".cnpt-table-meta")),e[1].id=d+"-vu-table",e[2].id=d+"-vu-table-breadcrumb",e};return{type:"TABLE",caption:"表格",renderClass:"",iconClass:"table",defaultWidth:500,defaultHeight:289,vm:{render:function(b){return a.render({id:b.rootId+b.serverData.id})}},entityDescription:c,processRenderData:d}});