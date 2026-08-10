"use client";

import { HomeHero } from "@/components/landing/home-hero";
import { HomeManifesto } from "@/components/landing/home-manifesto";
import { HomeExperience } from "@/components/landing/home-experience";
import { HomeFormations } from "@/components/landing/home-formations";
import { HomeMethodology } from "@/components/landing/home-methodology";
import { HomeAi } from "@/components/landing/home-ai";
import { HomePlatform } from "@/components/landing/home-platform";
import { HomeCta } from "@/components/landing/home-cta";
import { HomeFooter } from "@/components/landing/home-footer";

export default function Home() {
  return (
    <div className="flex flex-1 flex-col bg-navy-950">
      <HomeHero />
      <HomeManifesto />
      <HomeExperience />
      <HomeFormations />
      <HomeMethodology />
      <HomeAi />
      <HomePlatform />
      <HomeCta />
      <HomeFooter />
    </div>
  );
}
