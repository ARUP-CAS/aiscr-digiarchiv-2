import * as L from "leaflet";

declare module "leaflet" {
    namespace L {
        interface LocationFilter extends Layer {
            new(options?: LocationfilterOptions): any;
        }

        interface LocationfilterOptions {
            enable: boolean;
            adjustButton: boolean;
            buttonPosition: string;
            enableButton: {
                enableText: string;
                disableText: string;
            }
        }
        }
    namespace l {
        function locationFilter(options?: L.LocationfilterOptions): L.LocationFilter;
    }
}
