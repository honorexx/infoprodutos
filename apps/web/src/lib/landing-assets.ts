/**
 * Fotografias oficiais da home PKS — arquivos reais em /public/images/pks.
 * objectPosition calibrado para manter rostos/sujeitos visíveis no crop.
 * Fontes: PNGs de docs/Photos (sem recompressão agressiva).
 */
export const PKS_PHOTOS = {
  rooftop: {
    src: "/images/pks/rafael-rooftop.png",
    alt: "Vista editorial de rooftop ao entardecer — PKS Consultoria",
    objectPosition: "58% 42%",
    width: 1122,
    height: 1402,
  },
  bmw: {
    src: "/images/pks/pedro-bmw.png",
    alt: "Retrato executivo com horizonte urbano — PKS Consultoria",
    objectPosition: "50% 22%",
    width: 1023,
    height: 1537,
  },
  mercedes: {
    src: "/images/pks/rafael-mercedes.png",
    alt: "Retrato editorial em ambiente arquitetônico — PKS Consultoria",
    objectPosition: "50% 18%",
    width: 1086,
    height: 1448,
  },
} as const;

/** sizes tipicamente usados nas seções editoriais (retina). */
export const PKS_PHOTO_SIZES = {
  hero: "(max-width: 1024px) 100vw, 55vw",
  split: "(max-width: 1024px) 100vw, 50vw",
  portrait: "(max-width: 1024px) 92vw, 42vw",
} as const;
